/*
 * (C) Copyright 2010-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.video;

/**
 * Video constants.
 */
public class VideoConstants {

    public static final String VIDEO_TYPE = "Video";

    public static final String VIDEO_FACET = "Video";

    public static final String STORYBOARD_PROPERTY = "vid:storyboard";

    public static final String INFO_PROPERTY = "vid:info";

    public static final String DURATION_PROPERTY = "vid:info/duration";

    public static final String TRANSCODED_VIDEOS_PROPERTY = "vid:transcodedVideos";

    public static final String HAS_STORYBOARD_FACET = "HasStoryboard";

    public static final String HAS_VIDEO_PREVIEW_FACET = "HasVideoPreview";

    /**
     * @since 7.10
     */
    public static final String CTX_FORCE_INFORMATIONS_GENERATION = "forceInformationsGeneration";

    // Constant utility class.
    private VideoConstants() {
    }

}
