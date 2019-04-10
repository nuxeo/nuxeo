/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.liveconnect.onedrive;

import java.util.Objects;

import org.nuxeo.ecm.liveconnect.core.AbstractLiveConnectFile;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFileInfo;
import org.nuxeo.onedrive.client.OneDriveFile;

/**
 * @since 8.2
 */
public class OneDriveLiveConnectFile extends AbstractLiveConnectFile {

    private static final long serialVersionUID = 1L;

    private final String filename;

    private final long fileSize;

    private final String digest;

    public OneDriveLiveConnectFile(LiveConnectFileInfo info, OneDriveFile.Metadata file) {
        super(info);
        this.filename = Objects.requireNonNull(file.getName());
        this.fileSize = file.getSize();
        this.digest = Objects.requireNonNull(getDigest(file));
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

    private static String getDigest(OneDriveFile.Metadata file) {
        return file.getSha1Hash() == null ? file.getETag() : file.getSha1Hash();
    }

}
