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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableFile;

/**
 * Wraps an existing {@link Blob} to allow setting a different filename.
 *
 * @since 5.9.2
 */
public class BlobWrapper implements Blob, Serializable {

    private static final long serialVersionUID = 1L;

    protected final Blob blob;

    protected String filename;

    public BlobWrapper(Blob blob) {
        this.blob = blob;
        filename = blob.getFilename();
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public void setFilename(String filename) {
        this.filename = filename;
    }

    // the rest of the implementation just delegates to the blob

    @Override
    public String getMimeType() {
        return blob.getMimeType();
    }

    @Override
    public String getEncoding() {
        return blob.getEncoding();
    }

    @Override
    public String getDigest() {
        return blob.getDigest();
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
    public void setDigest(String digest) {
        blob.setDigest(digest);
    }

    @Override
    public InputStream getStream() throws IOException {
        return blob.getStream();
    }

    @Override
    public long getLength() {
        return blob.getLength();
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
    public void transferTo(OutputStream out) throws IOException {
        blob.transferTo(out);
    }

    @Override
    public void transferTo(File file) throws IOException {
        blob.transferTo(file);
    }

    @Override
    public File getFile() {
        return blob.getFile();
    }

    @Override
    public CloseableFile getCloseableFile() throws IOException {
        return blob.getCloseableFile();
    }

    @Override
    public CloseableFile getCloseableFile(String ext) throws IOException {
        return blob.getCloseableFile(ext);
    }

}
