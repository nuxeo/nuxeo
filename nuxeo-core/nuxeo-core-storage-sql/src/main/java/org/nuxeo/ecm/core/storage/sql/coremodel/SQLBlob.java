/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.DefaultStreamBlob;
import org.nuxeo.ecm.core.storage.sql.Binary;

/**
 * A {@link Blob} wrapping a {@link Binary} value.
 *
 * @author Florent Guillaume
 */
public class SQLBlob extends DefaultStreamBlob implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final Binary binary;

    public SQLBlob(Binary binary, String filename, String mimeType,
            String encoding, String digest) {
        this.binary = binary;
        setFilename(filename);
        setMimeType(mimeType);
        setEncoding(encoding);
        setDigest(digest);
    }

    @Override
    public long getLength() {
        return binary.getLength();
    }

    public InputStream getStream() throws IOException {
        return binary.getStream();
    }

    public boolean isPersistent() {
        return true;
    }

    public Blob persist() {
        return this;
    }

}
