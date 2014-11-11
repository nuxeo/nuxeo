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
 */
package org.nuxeo.ecm.automation.client.jaxrs.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.ecm.automation.client.jaxrs.util.Base64;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class Blob implements OperationInput {

    public static Blob fromBase64String(String fileName, String content) {
        return fromBase64String(fileName, content, null);
    }

    public static Blob fromBase64String(String fileName, String content, String mimeType) {
        return new StreamBlob(new ByteArrayInputStream(Base64.decode(content)), fileName, mimeType);
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
        this.mimeType = mimeType == null ? "application/octet-stream"
                : mimeType;
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
            return "0 B";
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
