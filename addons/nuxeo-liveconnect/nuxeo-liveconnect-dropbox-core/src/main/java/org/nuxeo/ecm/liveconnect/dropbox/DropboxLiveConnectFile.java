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
package org.nuxeo.ecm.liveconnect.dropbox;

import java.util.Objects;

import org.nuxeo.ecm.liveconnect.core.AbstractLiveConnectFile;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFileInfo;

import com.dropbox.core.v2.files.FileMetadata;

/**
 * @since 8.1
 */
public class DropboxLiveConnectFile extends AbstractLiveConnectFile {

    private static final long serialVersionUID = 1L;

    private final FileMetadata file;

    public DropboxLiveConnectFile(LiveConnectFileInfo info, FileMetadata file) {
        super(info);
        this.file = Objects.requireNonNull(file);
    }

    @Override
    public String getFilename() {
        return file.getName();
    }

    @Override
    public long getFileSize() {
        return file.getSize();
    }

    @Override
    public String getDigest() {
        return file.getRev();
    }
}
