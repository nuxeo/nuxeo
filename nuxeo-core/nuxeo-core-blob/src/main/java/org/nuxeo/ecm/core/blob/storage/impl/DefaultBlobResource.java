/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.blob.storage.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.ecm.core.blob.storage.BlobResource;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultBlobResource implements BlobResource {

    protected File file;
    protected String hash;
    protected long lastModified;

    public DefaultBlobResource(File file, String hash) {
        this (file, hash, file.lastModified());
    }

    public DefaultBlobResource(File file, String hash, long lastModified) {
        this.file = file;
        this.hash = hash;
        this.lastModified = lastModified;
    }

    public long lastModified() {
        return lastModified;
    }

    public String getHash() {
        return hash;
    }

    public InputStream getStream() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof DefaultBlobResource) {
            return ((DefaultBlobResource)obj).file.equals(file);
        }
        return false;
    }
}
