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
package org.nuxeo.ecm.liveconnect.google.drive;

import java.util.Objects;

import org.nuxeo.ecm.liveconnect.core.AbstractLiveConnectFile;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFileInfo;

import com.google.api.services.drive.model.File;

/**
 * @since 8.1
 */
public class GoogleDriveLiveConnectFile extends AbstractLiveConnectFile {

    private static final long serialVersionUID = 1L;

    private final String filename;

    private final long fileSize;

    private final String digest;

    private final String mimeType;

    public GoogleDriveLiveConnectFile(LiveConnectFileInfo fileInfo, File file) {
        super(fileInfo);
        this.filename = Objects.requireNonNull(file.getTitle());
        this.fileSize = file.getFileSize() != null ? file.getFileSize() : -1;
        this.digest = Objects.requireNonNull(getDigest(file));
        this.mimeType = Objects.requireNonNull(file.getMimeType());
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

    @Override
    public String getMimeType() {
        return mimeType;
    }

    public static String getDigest(File file) {
        return file.getMd5Checksum() == null ? file.getEtag() : file.getMd5Checksum();
    }

}
