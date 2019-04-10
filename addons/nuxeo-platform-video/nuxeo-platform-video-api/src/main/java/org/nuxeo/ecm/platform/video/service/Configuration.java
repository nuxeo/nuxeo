package org.nuxeo.ecm.platform.video.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Configuration of the {@link VideoService}.
 * <p>
 * Contains
 *
 * @since 7.4
 */
@XObject("configuration")
public class Configuration {

    public static final Configuration DEFAULT_CONFIGURATION = new Configuration();

    @XNode("previewScreenshotInDurationPercent")
    protected double previewScreenshotInDurationPercent = 10.0;

    @XNode("storyboardMinDuration")
    protected double storyboardMinDuration = 10.0;

    @XNode("storyboardThumbnailCount")
    protected int storyboardThumbnailCount = 9;

    public double getPreviewScreenshotInDurationPercent() {
        return previewScreenshotInDurationPercent;
    }

    public int getStoryboardThumbnailCount() {
        return storyboardThumbnailCount;
    }

    public double getStoryboardMinDuration() {
        return storyboardMinDuration;
    }
}
