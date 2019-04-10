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
package org.nuxeo.ecm.liveconnect.box;

import java.util.Objects;

import org.nuxeo.ecm.liveconnect.core.AbstractLiveConnectFile;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFileInfo;

import com.box.sdk.BoxFile.Info;

/**
 * @since 8.1
 */
public class BoxLiveConnectFile extends AbstractLiveConnectFile {

    private static final long serialVersionUID = 1L;

    private final String filename;

    private final long fileSize;

    private final String digest;

    public BoxLiveConnectFile(LiveConnectFileInfo info, Info file) {
        super(info);
        this.filename = Objects.requireNonNull(file.getName());
        this.fileSize = file.getSize();
        this.digest = Objects.requireNonNull(file.getSha1());
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
