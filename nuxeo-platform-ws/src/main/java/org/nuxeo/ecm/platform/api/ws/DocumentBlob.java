/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: DocumentBlob.java 13220 2007-03-03 18:45:30Z bstefanescu $
 */

package org.nuxeo.ecm.platform.api.ws;

import java.io.IOException;
import java.io.Serializable;

import org.nuxeo.ecm.core.api.Blob;

/**
 * Web service document blob.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public class DocumentBlob implements Serializable {

    private static final long serialVersionUID = 1L;

    private String encoding;

    private String mimeType;

    private String[] extensions;

    private String name;

    private byte[] blob;

    private String url;

    /**
     * Empty ctor needed by tools like jaxb.
     */
    public DocumentBlob() {
    }

    public DocumentBlob(String name, Blob blob) throws IOException {
        this.blob = blob.getByteArray();
        encoding = blob.getEncoding();
        mimeType = blob.getMimeType();
        this.name = name;
    }

    public DocumentBlob(String name, String encoding, String mimeType, String downloadUrl) {
        this.name = name;
        this.encoding = encoding;
        this.mimeType = mimeType;
        url = downloadUrl;
    }

    /**
     * Returns the name of the document field name.
     * <p>
     * We probably need to embed the name along with the schema prefix.
     *
     * @return the name of the document field name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the main RFC-2046 mimetype name.
     *
     * @return the main RFC-2046 name
     */
    public String getMimetype() {
        return mimeType;
    }

    /**
     * Returns the encoding of the blob.
     *
     * @return the encoding of the blob
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Returns the actual blob as a serializable input stream.
     *
     * @return the actual blob as a serializable input stream
     */
    public byte[] getBlob() {
        return blob;
    }

    public void setExtensions(String[] extensions) {
        this.extensions = extensions;
    }

    public String[] getExtensions() {
        return extensions;
    }

    public void setBlob(byte[] blob) {
        this.blob = blob;
    }

    public void setMimetype(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDownloadUrl() {
        return url;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
