/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.liveconnect.core;

/**
 * @since 8.1
 */
public class MockLiveConnectFile extends AbstractLiveConnectFile {

    private static final long serialVersionUID = 1L;

    private String filename;

    private long fileSize;

    private String digest;

    public MockLiveConnectFile(LiveConnectFileInfo info, String filename, long fileSize, String digest) {
        super(info);
        this.filename = filename;
        this.fileSize = fileSize;
        this.digest = digest;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public long getFileSize() {
        return fileSize;
    }

    @Override
    public String getDigest() {
        return digest;
    }

}
