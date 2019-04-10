/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.liveconnect.core;

/**
 * An empty {@link LiveConnectFile} returned when there is an underlying {@link IOException}.
 *
 * @since 9.10
 */
public class ErrorLiveConnectFile extends AbstractLiveConnectFile {

    private static final long serialVersionUID = 1L;

    public static final String FILENAME = "error.bin";

    public static final String MIME_TYPE = "application/error";

    public ErrorLiveConnectFile(LiveConnectFileInfo info) {
        super(info);
    }

    @Override
    public String getFilename() {
        return FILENAME;
    }

    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    @Override
    public long getFileSize() {
        return 0;
    }

    @Override
    public String getDigest() {
        return "";
    }

}
