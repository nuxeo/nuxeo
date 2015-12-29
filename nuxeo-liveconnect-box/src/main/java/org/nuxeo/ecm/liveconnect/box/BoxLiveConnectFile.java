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

import com.box.sdk.BoxFile;
import com.box.sdk.BoxFile.Info;

/**
 * @since 8.1
 */
public class BoxLiveConnectFile extends AbstractLiveConnectFile {

    private final BoxFile.Info file;

    public BoxLiveConnectFile(Info file) {
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
        return file.getSha1();
    }
}
