/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thomas Roger
 */
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
