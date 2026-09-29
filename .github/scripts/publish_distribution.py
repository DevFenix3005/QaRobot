#!/usr/bin/env python3
"""Publish a tested distribution without replacing existing release assets."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
from urllib.error import HTTPError, URLError
from urllib.parse import quote
from urllib.request import Request, urlopen


TAG_PATTERN = re.compile(
    r"v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)"
    r"(-[0-9A-Za-z]+([.-][0-9A-Za-z]+)*)?"
)
REPOSITORY_PATTERN = re.compile(
    r"[A-Za-z0-9][A-Za-z0-9_.-]*/[A-Za-z0-9][A-Za-z0-9_.-]*"
)


class PublicationError(Exception):
    """A distribution cannot safely be published."""


def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def validate_assets(tag, directory):
    if not TAG_PATTERN.fullmatch(tag):
        raise PublicationError("Release tags must have the form v2.1.0 or v2.1.0-rc.1.")
    directory = Path(directory)
    archive = directory / f"QaRobot-{tag[1:]}.zip"
    manifest = directory / "SHA256SUMS.txt"
    if not directory.is_dir() or not archive.is_file() or not manifest.is_file():
        raise PublicationError(f"Expected {archive.name} and SHA256SUMS.txt in {directory}.")
    if len(list(directory.glob("*.zip"))) != 1:
        raise PublicationError("Expected exactly one distribution ZIP in the assets directory.")
    try:
        manifest_text = manifest.read_text(encoding="utf-8")
    except UnicodeError as error:
        raise PublicationError("SHA256SUMS.txt must contain UTF-8 text.") from error
    match = re.fullmatch(
        r"([0-9a-fA-F]{64}) [ *]" + re.escape(archive.name) + r"\n?", manifest_text
    )
    if match is None:
        raise PublicationError(f"SHA256SUMS.txt must contain exactly one checksum for {archive.name}.")
    archive_hash = sha256(archive)
    if archive_hash != match.group(1).lower():
        raise PublicationError(f"SHA-256 verification failed for {archive.name}.")
    return [(archive, archive_hash), (manifest, sha256(manifest))]


def fetch_release(repository, tag):
    token = os.environ.get("GH_TOKEN") or os.environ.get("GITHUB_TOKEN")
    if not token:
        raise PublicationError("Set GH_TOKEN or GITHUB_TOKEN to query and publish the release.")
    api_url = os.environ.get("GITHUB_API_URL", "https://api.github.com").rstrip("/")
    owner, name = repository.split("/", 1)
    endpoint = (
        f"{api_url}/repos/{quote(owner, safe='')}/{quote(name, safe='')}"
        f"/releases/tags/{quote(tag, safe='')}"
    )
    request = Request(endpoint, headers={
        "Accept": "application/vnd.github+json",
        "Authorization": f"Bearer {token}",
        "User-Agent": "QaRobot-distribution-publisher",
    })
    try:
        with urlopen(request, timeout=30) as response:
            release = json.load(response)
    except HTTPError as error:
        if error.code == 404:
            return None
        raise PublicationError(f"GitHub release lookup failed with HTTP {error.code}.") from error
    except (URLError, TimeoutError) as error:
        raise PublicationError(f"GitHub release lookup failed: {error}.") from error
    except (ValueError, UnicodeError) as error:
        raise PublicationError("GitHub returned an invalid release response.") from error
    if not isinstance(release, dict) or release.get("tag_name") != tag:
        raise PublicationError("GitHub returned an unexpected release or tag.")
    return release


def run_gh(arguments):
    try:
        subprocess.run(["gh", *arguments], check=True)
    except FileNotFoundError as error:
        raise PublicationError("GitHub CLI (gh) is required to publish the distribution.") from error
    except subprocess.CalledProcessError as error:
        raise PublicationError(f"GitHub CLI failed with exit code {error.returncode}; existing assets were not replaced.") from error


def publish_distribution(tag, directory, repository):
    if not repository or not REPOSITORY_PATTERN.fullmatch(repository):
        raise PublicationError("Specify the repository as OWNER/REPO.")
    assets = validate_assets(tag, directory)
    release = fetch_release(repository, tag)
    if release is None:
        arguments = [
            "release", "create", tag, *(str(path) for path, _ in assets),
            "--verify-tag", "--title", f"QaRobot {tag}", "--generate-notes",
            "--repo", repository,
        ]
        if "-" in tag:
            arguments.append("--prerelease")
        run_gh(arguments)
        print(f"Created release {tag} with the verified distribution.")
        return

    remote_assets = release.get("assets")
    if not isinstance(remote_assets, list) or any(not isinstance(asset, dict) for asset in remote_assets):
        raise PublicationError("GitHub returned an invalid release asset list.")
    names = [asset.get("name") for asset in remote_assets]
    for path, _ in assets:
        if names.count(path.name) > 1:
            raise PublicationError(f"Release {tag} contains duplicate assets named {path.name}.")
    missing = [path for path, _ in assets if path.name not in names]
    if missing and release.get("immutable"):
        raise PublicationError(f"Release {tag} is immutable; missing assets cannot be uploaded.")

    # Validate every existing asset before uploading anything to a partial release.
    with tempfile.TemporaryDirectory(prefix="qarobot-release-") as temporary:
        for path, expected_hash in assets:
            if path.name not in names:
                continue
            run_gh([
                "release", "download", tag, "--pattern", path.name,
                "--dir", temporary, "--repo", repository,
            ])
            downloaded = Path(temporary) / path.name
            if not downloaded.is_file():
                raise PublicationError(f"GitHub CLI did not download the existing asset {path.name}.")
            actual_hash = sha256(downloaded)
            if actual_hash != expected_hash:
                raise PublicationError(
                    f"Release {tag} already contains different content for {path.name} "
                    f"(existing SHA-256: {actual_hash}; local SHA-256: {expected_hash}). "
                    "No assets were uploaded. Publish different content under a new version tag."
                )
            print(f"Keeping identical asset: {path.name}")

    if missing:
        run_gh(["release", "upload", tag, *(str(path) for path in missing), "--repo", repository])
        print(f"Uploaded missing assets to {tag}: {', '.join(path.name for path in missing)}")
    else:
        print(f"Release {tag} already contains the verified distribution; no changes needed.")


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tag", required=True)
    parser.add_argument("--assets", required=True, type=Path)
    parser.add_argument("--repository", default=os.environ.get("GH_REPO") or os.environ.get("GITHUB_REPOSITORY"))
    arguments = parser.parse_args(argv)
    try:
        publish_distribution(arguments.tag, arguments.assets, arguments.repository)
    except (PublicationError, OSError) as error:
        print(f"Error: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
