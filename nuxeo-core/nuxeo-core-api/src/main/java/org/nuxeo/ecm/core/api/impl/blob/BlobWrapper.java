/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.core.api.impl.blob;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;

import org.nuxeo.ecm.core.api.Blob;

/**
 * Wraps an existing Blob allowing to set a different filename than the original
 * Blob's filename.
 *
 * @since 5.9.2
 */
public class BlobWrapper extends AbstractBlob implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final Blob blob;

    protected String filename;

    public BlobWrapper(Blob blob) {
        this.blob = blob;
    }

    public BlobWrapper(Blob blob, String filename) {
        this.blob = blob;
        this.filename = filename;
    }

    @Override
    public long getLength() {
        return blob.getLength();
    }

    @Override
    public String getEncoding() {
        return blob.getEncoding();
    }

    @Override
    public String getMimeType() {
        return blob.getMimeType();
    }

    @Override
    public String getFilename() {
        return filename != null ? filename : blob.getFilename();
    }

    @Override
    public String getDigest() {
        return blob.getDigest();
    }

    @Override
    public void setDigest(String digest) {
        blob.setDigest(digest);
    }

    @Override
    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Override
    public void setMimeType(String mimeType) {
        blob.setMimeType(mimeType);
    }

    @Override
    public void setEncoding(String encoding) {
        blob.setEncoding(encoding);
    }

    @Override
    public InputStream getStream() throws IOException {
        return blob.getStream();
    }

    @Override
    public Reader getReader() throws IOException {
        return blob.getReader();
    }

    @Override
    public byte[] getByteArray() throws IOException {
        return blob.getByteArray();
    }

    @Override
    public String getString() throws IOException {
        return blob.getString();
    }

    @Override
    public Blob persist() throws IOException {
        return blob.persist();
    }

    @Override
    public boolean isPersistent() {
        return blob.isPersistent();
    }
}
