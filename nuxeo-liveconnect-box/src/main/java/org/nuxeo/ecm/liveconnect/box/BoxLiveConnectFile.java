/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Kevin Leturc
 */
package org.nuxeo.ecm.liveconnect.box;

import java.util.Objects;

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
