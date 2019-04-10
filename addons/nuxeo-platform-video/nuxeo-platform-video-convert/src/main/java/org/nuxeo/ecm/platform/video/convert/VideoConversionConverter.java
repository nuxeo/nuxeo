/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
