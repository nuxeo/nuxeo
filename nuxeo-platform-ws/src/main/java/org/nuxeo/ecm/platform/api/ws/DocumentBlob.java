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


/**
 * Web service document blob.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class DocumentBlob implements Serializable {

    private static final long serialVersionUID = 1L;

    private String encoding;

    private String mimeType;

    private String[] extensions;

    private String name;

    private byte[] blob;



    /**
     * Emtpy ctor needed by tools like jaxb.
     */
    public DocumentBlob() {
        // TODO Auto-generated constructor stub
    }

    public DocumentBlob(String name, org.nuxeo.ecm.core.api.Blob blob) throws IOException {
        this.blob = blob.getByteArray();
        encoding = blob.getEncoding();
        mimeType = blob.getMimeType();
        this.name = name;
    }


    /**
     * Returns the name of the document field name.
     * <p>
     * We probably need to embeed the name along with the schema prefix.
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


    /**
     * @param extensions the extensions to set.
     */
    public void setExtensions(String[] extensions) {
        this.extensions = extensions;
    }

    public String[] getExtensions() {
        return extensions;
    }

    /**
     * @param blob the blob to set.
     */
    public void setBlob(byte[] blob) {
        this.blob = blob;
    }

    /**
     * @param mimeType the mimeType to set.
     */
    public void setMimetype(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @param encoding the encoding to set.
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * @param name the name to set.
     */
    public void setName(String name) {
        this.name = name;
    }


}
