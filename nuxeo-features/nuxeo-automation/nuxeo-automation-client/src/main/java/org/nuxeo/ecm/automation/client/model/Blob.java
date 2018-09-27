/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.codec.binary.Base64;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class Blob implements OperationInput {

    private static final long serialVersionUID = 1L;

    public static Blob fromBase64String(String fileName, String content) {
        return fromBase64String(fileName, content, null);
    }

    public static Blob fromBase64String(String fileName, String content, String mimeType) {
        return new StreamBlob(new ByteArrayInputStream(Base64.decodeBase64(content)), fileName, mimeType);
    }

    protected String mimeType;

    protected String fileName;

    protected Blob() {

    }

    public Blob(String fileName) {
        this(fileName, null);
    }

    public Blob(String fileName, String mimeType) {
        this.fileName = fileName;
        setMimeType(mimeType);
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType == null ? "application/octet-stream" : mimeType;
    }

    public int getLength() {
        return -1;
    }

    public String getInputType() {
        return "blob";
    }

    public String getInputRef() {
        return null;
    }

    public boolean isBinary() {
        return true;
    }

    public abstract InputStream getStream() throws IOException;

    protected String formatLength(int len) {
        int k = len / 1024;
        if (k <= 0) {
            return len + " B";
        } else if (k < 1024) {
            return k + " K";
        } else {
            return (k / 1024) + " M";
        }
    }

    @Override
    public String toString() {
        return fileName + " - " + mimeType + " - " + formatLength(getLength());
    }
}
