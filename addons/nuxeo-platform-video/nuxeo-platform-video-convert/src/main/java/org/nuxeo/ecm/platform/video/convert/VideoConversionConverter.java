package org.nuxeo.ecm.platform.video.convert;

/**
 * Generic video convert configured by converter parameters contributions.
 *
 * @since 5.9.5
 */
public class VideoConversionConverter extends BaseVideoConversionConverter {

    public static final String VIDEO_MIME_TYPE_KEY = "videoMimeType";

    public static final String VIDEO_EXTENSION_KEY = "videoExtension";

    public static final String VIDEO_TMP_DIRECTORY_PREFIX_KEY = "tmpDirectoryPrefix";

    @Override
    protected String getVideoMimeType() {
        return initParameters.get(VIDEO_MIME_TYPE_KEY);
    }

    @Override
    protected String getVideoExtension() {
        String extension = initParameters.get(VIDEO_EXTENSION_KEY);
        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }
        return extension;
    }

    @Override
    protected String getTmpDirectoryPrefix() {
        return initParameters.get(VIDEO_TMP_DIRECTORY_PREFIX_KEY);
    }
}
