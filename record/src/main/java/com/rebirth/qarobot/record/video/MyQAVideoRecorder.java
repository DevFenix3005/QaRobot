package com.rebirth.qarobot.record.video;

import lombok.extern.slf4j.Slf4j;
import org.monte.media.Format;
import org.monte.media.FormatKeys;
import org.monte.media.Registry;
import org.monte.media.math.Rational;
import org.monte.screenrecorder.ScreenRecorder;

import java.awt.AWTException;
import java.awt.GraphicsConfiguration;
import java.io.File;
import java.io.IOException;

import static org.monte.media.FormatKeys.EncodingKey;
import static org.monte.media.FormatKeys.FrameRateKey;
import static org.monte.media.FormatKeys.KeyFrameIntervalKey;
import static org.monte.media.FormatKeys.MIME_AVI;
import static org.monte.media.FormatKeys.MediaType;
import static org.monte.media.FormatKeys.MediaTypeKey;
import static org.monte.media.FormatKeys.MimeTypeKey;
import static org.monte.media.VideoFormatKeys.CompressorNameKey;
import static org.monte.media.VideoFormatKeys.DepthKey;
import static org.monte.media.VideoFormatKeys.ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE;
import static org.monte.media.VideoFormatKeys.QualityKey;

@Slf4j
public class MyQAVideoRecorder extends ScreenRecorder {

    public MyQAVideoRecorder(GraphicsConfiguration cfg, File movieFolder) throws IOException, AWTException {
        super(cfg, null,
                new Format(
                        MediaTypeKey, FormatKeys.MediaType.FILE,
                        MimeTypeKey, MIME_AVI
                ),
                new Format(
                        MediaTypeKey, MediaType.VIDEO,
                        EncodingKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
                        CompressorNameKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
                        DepthKey, 24,
                        FrameRateKey, Rational.valueOf(15),
                        QualityKey, 1.0f,
                        KeyFrameIntervalKey, 15 * 60
                ),
                new Format(
                        MediaTypeKey, MediaType.VIDEO,
                        EncodingKey, ENCODING_YELLOW_CURSOR,
                        FrameRateKey, Rational.valueOf(30)
                ),
                null,
                movieFolder);
    }

    @Override
    protected File createMovieFile(Format fileFormat) throws IOException {
        return new File(movieFolder, "video." + Registry.getInstance().getExtension(fileFormat));
    }
}
