/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
    public String getDigestAlgorithm() {
        return blob.getDigestAlgorithm();
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
