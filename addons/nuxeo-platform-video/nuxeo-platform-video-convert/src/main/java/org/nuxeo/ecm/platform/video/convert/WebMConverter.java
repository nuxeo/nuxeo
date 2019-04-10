/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.video.convert;

/**
 * Convert to WebM format.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 * @deprecated since 5.9.5. Use {@link org.nuxeo.ecm.platform.video.convert.VideoConversionConverter}.
 */
@Deprecated
public class WebMConverter extends BaseVideoConversionConverter {

    public static final String WEBM_VIDEO_MIMETYPE = "video/webm";

    public static final String WEBM_EXTENSION = ".webm";

    public static final String TMP_DIRECTORY_PREFIX = "convertToWebM";

    @Override
    protected String getVideoMimeType() {
        return WEBM_VIDEO_MIMETYPE;
    }

    @Override
    protected String getVideoExtension() {
        return WEBM_EXTENSION;
    }

    @Override
    protected String getTmpDirectoryPrefix() {
        return TMP_DIRECTORY_PREFIX;
    }
}
