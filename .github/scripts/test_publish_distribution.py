"""Offline regression tests for publishing and retrying GitHub Releases."""

import hashlib
import io
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from urllib.error import HTTPError, URLError

import publish_distribution as publisher


class PublishDistributionTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory(prefix="qarobot-publisher-test-")
        self.addCleanup(temporary.cleanup)
        self.assets = Path(temporary.name)
        self.tag = "v2.2.0-op.1"
        self.repository = "DevFenix3005/QaRobot"
        self.archive = self.assets / "QaRobot-2.2.0-op.1.zip"
        self.archive.write_bytes(b"tested distribution")
        self.manifest = self.assets / "SHA256SUMS.txt"
        self.manifest.write_text(
            f"{hashlib.sha256(self.archive.read_bytes()).hexdigest()}  {self.archive.name}\n",
            encoding="utf-8", newline="\n",
        )
        self.remote_content = {}
        self.commands = []
        self.lookup = self.enterContext(patch.object(publisher, "fetch_release"))
        self.enterContext(patch.object(publisher, "run_gh", side_effect=self.fake_gh))
        self.enterContext(patch("sys.stdout", new_callable=io.StringIO))

    def fake_gh(self, arguments):
        self.commands.append(arguments)
        if arguments[:2] == ["release", "download"]:
            name = arguments[arguments.index("--pattern") + 1]
            destination = Path(arguments[arguments.index("--dir") + 1]) / name
            destination.write_bytes(self.remote_content[name])

    def release(self, *paths, immutable=False):
        self.remote_content = {path.name: path.read_bytes() for path in paths}
        self.lookup.return_value = {
            "tag_name": self.tag, "assets": [{"name": path.name} for path in paths],
            "immutable": immutable,
        }

    def publish(self):
        publisher.publish_distribution(self.tag, self.assets, self.repository)

    def mutations(self):
        return [command for command in self.commands if command[1] in ("create", "upload")]

    def add_windows_installer(self):
        self.installer = self.assets / "QaRobot-2.2.0-op.1-windows-x64.exe"
        self.installer.write_bytes(b"tested native Windows installer")
        self.windows_manifest = self.assets / "SHA256SUMS-windows.txt"
        self.windows_manifest.write_text(
            f"{publisher.sha256(self.installer)}  {self.installer.name}\n",
            encoding="utf-8", newline="\n",
        )

    def test_creates_new_prerelease_with_verified_tag(self):
        self.lookup.return_value = None
        self.publish()
        self.assertEqual(len(self.commands), 1)
        self.assertEqual(self.commands[0], [
            "release", "create", self.tag, str(self.archive), str(self.manifest),
            "--verify-tag", "--title", f"QaRobot {self.tag}", "--generate-notes",
            "--repo", self.repository, "--prerelease",
        ])

    def test_stable_release_is_not_marked_prerelease(self):
        stable_archive = self.assets / "QaRobot-2.2.0.zip"
        self.archive.rename(stable_archive)
        self.manifest.write_text(
            f"{publisher.sha256(stable_archive)}  {stable_archive.name}\n", encoding="utf-8"
        )
        self.lookup.return_value = None
        publisher.publish_distribution("v2.2.0", self.assets, self.repository)
        self.assertNotIn("--prerelease", self.commands[0])

    def test_empty_release_gets_both_assets_without_replacement(self):
        self.release()
        self.publish()
        self.assertEqual(self.commands, [[
            "release", "upload", self.tag, str(self.archive), str(self.manifest), "--repo", self.repository,
        ]])
        self.assertNotIn("--clobber", self.commands[0])

    def test_partial_release_keeps_zip_and_uploads_only_missing_manifest(self):
        self.release(self.archive)
        self.publish()
        self.assertEqual(self.commands[0][:2], ["release", "download"])
        self.assertEqual(self.mutations(), [[
            "release", "upload", self.tag, str(self.manifest), "--repo", self.repository,
        ]])

    def test_identical_release_is_idempotent(self):
        self.release(self.archive, self.manifest)
        self.publish()
        self.assertEqual(len(self.commands), 2)
        self.assertEqual(self.mutations(), [])

    def test_conflict_prevents_upload_of_other_missing_asset(self):
        self.release(self.manifest)
        self.remote_content[self.manifest.name] = b"different checksum manifest"
        with self.assertRaisesRegex(publisher.PublicationError, "different content for SHA256SUMS"):
            self.publish()
        self.assertEqual(self.mutations(), [])

    def test_conflict_after_first_matching_asset_prevents_mutation(self):
        self.release(self.archive, self.manifest)
        self.remote_content[self.manifest.name] = b"different checksum manifest"
        with self.assertRaises(publisher.PublicationError):
            self.publish()
        self.assertEqual(len(self.commands), 2)
        self.assertEqual(self.mutations(), [])

    def test_duplicate_expected_asset_is_rejected(self):
        self.release(self.archive, self.archive)
        with self.assertRaisesRegex(publisher.PublicationError, "duplicate assets"):
            self.publish()
        self.assertEqual(self.commands, [])

    def test_immutable_release_cannot_receive_missing_assets(self):
        self.release(immutable=True)
        with self.assertRaisesRegex(publisher.PublicationError, "immutable"):
            self.publish()
        self.assertEqual(self.commands, [])

    def test_complete_immutable_release_can_be_verified(self):
        self.release(self.archive, self.manifest, immutable=True)
        self.publish()
        self.assertEqual(self.mutations(), [])

    def test_bad_checksum_fails_before_api_lookup(self):
        self.archive.write_bytes(b"changed after checksum generation")
        with self.assertRaisesRegex(publisher.PublicationError, "SHA-256 verification failed"):
            self.publish()
        self.lookup.assert_not_called()
        self.assertEqual(self.commands, [])

    def test_manifest_cannot_reference_another_file(self):
        self.manifest.write_text("0" * 64 + "  ../another.zip\n", encoding="utf-8")
        with self.assertRaisesRegex(publisher.PublicationError, "exactly one checksum"):
            self.publish()
        self.lookup.assert_not_called()

    def test_missing_distribution_fails_before_api_lookup(self):
        self.archive.unlink()
        with self.assertRaisesRegex(publisher.PublicationError, "Expected QaRobot"):
            self.publish()
        self.lookup.assert_not_called()

    def test_invalid_tag_fails_before_api_lookup(self):
        with self.assertRaisesRegex(publisher.PublicationError, "Release tags"):
            publisher.publish_distribution("v2.2.0;bad", self.assets, self.repository)
        self.lookup.assert_not_called()

    def test_api_failure_never_attempts_creation(self):
        self.lookup.side_effect = publisher.PublicationError("HTTP 403")
        with self.assertRaisesRegex(publisher.PublicationError, "HTTP 403"):
            self.publish()
        self.assertEqual(self.commands, [])

    def test_new_release_includes_zip_and_verified_windows_installer(self):
        self.add_windows_installer()
        self.lookup.return_value = None
        self.publish()
        self.assertEqual(self.commands[0][:7], [
            "release", "create", self.tag, str(self.archive), str(self.manifest),
            str(self.installer), str(self.windows_manifest),
        ])
        self.assertIn("--verify-tag", self.commands[0])

    def test_existing_zip_release_receives_only_windows_pair(self):
        self.add_windows_installer()
        self.release(self.archive, self.manifest)
        self.publish()
        self.assertEqual(self.mutations(), [[
            "release", "upload", self.tag, str(self.installer), str(self.windows_manifest),
            "--repo", self.repository,
        ]])

    def test_complete_windows_release_is_idempotent(self):
        self.add_windows_installer()
        self.release(self.archive, self.manifest, self.installer, self.windows_manifest)
        self.publish()
        self.assertEqual(len(self.commands), 4)
        self.assertEqual(self.mutations(), [])

    def test_partial_windows_release_receives_only_missing_manifest(self):
        self.add_windows_installer()
        self.release(self.archive, self.manifest, self.installer)
        self.publish()
        self.assertEqual(self.mutations(), [[
            "release", "upload", self.tag, str(self.windows_manifest), "--repo", self.repository,
        ]])

    def test_windows_conflict_prevents_uploading_any_other_asset(self):
        self.add_windows_installer()
        self.release(self.installer)
        self.remote_content[self.installer.name] = b"different native installer"
        with self.assertRaisesRegex(publisher.PublicationError, "different content"):
            self.publish()
        self.assertEqual(self.mutations(), [])

    def test_missing_windows_manifest_fails_before_api_lookup(self):
        self.add_windows_installer()
        self.windows_manifest.unlink()
        with self.assertRaisesRegex(publisher.PublicationError, "together"):
            self.publish()
        self.lookup.assert_not_called()
        self.assertEqual(self.commands, [])

    def test_missing_windows_installer_fails_before_api_lookup(self):
        self.add_windows_installer()
        self.installer.unlink()
        with self.assertRaisesRegex(publisher.PublicationError, "together"):
            self.publish()
        self.lookup.assert_not_called()
        self.assertEqual(self.commands, [])

    def test_windows_checksum_mismatch_fails_before_api_lookup(self):
        self.add_windows_installer()
        self.installer.write_bytes(b"changed after checksum generation")
        with self.assertRaisesRegex(publisher.PublicationError, "SHA-256 verification failed"):
            self.publish()
        self.lookup.assert_not_called()
        self.assertEqual(self.commands, [])

    def test_windows_manifest_cannot_reference_another_file(self):
        self.add_windows_installer()
        self.windows_manifest.write_text("0" * 64 + "  ../another.exe\n", encoding="utf-8")
        with self.assertRaisesRegex(publisher.PublicationError, "exactly one checksum"):
            self.publish()
        self.lookup.assert_not_called()

    def test_unexpected_installer_version_fails_before_api_lookup(self):
        (self.assets / "QaRobot-2.1.0-windows-x64.exe").write_bytes(b"wrong version")
        with self.assertRaisesRegex(publisher.PublicationError, "Unexpected release assets"):
            self.publish()
        self.lookup.assert_not_called()

    def test_extra_archive_is_rejected_before_api_lookup(self):
        (self.assets / "QaRobot-2.1.0.zip").write_bytes(b"wrong version")
        with self.assertRaisesRegex(publisher.PublicationError, "Unexpected release assets"):
            self.publish()
        self.lookup.assert_not_called()


class FetchReleaseTest(unittest.TestCase):
    def setUp(self):
        self.enterContext(patch.dict(os.environ, {
            "GH_TOKEN": "test-token", "GITHUB_API_URL": "https://api.github.test",
        }))
        self.urlopen = self.enterContext(patch.object(publisher, "urlopen"))

    def test_404_is_the_only_missing_release_response(self):
        self.urlopen.side_effect = HTTPError("https://api.github.test", 404, "Not Found", {}, None)
        self.assertIsNone(publisher.fetch_release("owner/repo", "v2.2.0"))

    def test_403_is_not_a_missing_release(self):
        self.urlopen.side_effect = HTTPError("https://api.github.test", 403, "Forbidden", {}, None)
        with self.assertRaisesRegex(publisher.PublicationError, "HTTP 403"):
            publisher.fetch_release("owner/repo", "v2.2.0")

    def test_network_failure_is_not_a_missing_release(self):
        self.urlopen.side_effect = URLError("network unavailable")
        with self.assertRaisesRegex(publisher.PublicationError, "network unavailable"):
            publisher.fetch_release("owner/repo", "v2.2.0")

    def test_request_uses_configured_api_token_and_returns_release(self):
        release = {"tag_name": "v2.2.0", "assets": []}
        self.urlopen.return_value = io.BytesIO(json.dumps(release).encode())
        self.assertEqual(publisher.fetch_release("owner/repo", "v2.2.0"), release)
        request = self.urlopen.call_args.args[0]
        self.assertEqual(request.full_url, "https://api.github.test/repos/owner/repo/releases/tags/v2.2.0")
        self.assertEqual(request.get_header("Authorization"), "Bearer test-token")
        self.assertEqual(self.urlopen.call_args.kwargs["timeout"], 30)


if __name__ == "__main__":
    unittest.main()
