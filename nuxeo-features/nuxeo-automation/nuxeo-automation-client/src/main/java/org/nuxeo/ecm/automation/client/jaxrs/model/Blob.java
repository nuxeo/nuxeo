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

import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class Blob implements OperationInput {

    protected String mimeType;

    protected String fileName;

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

}
