/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

/**
 * An in memory bob containing a string.
 *
 * @author bstefanescu
 */
public class StringBlob extends Blob {

    /** content */
    protected final String content;

    protected String charset;


    public StringBlob(String content) {
        this.content = content;
    }

    /** Creates a <code>StringBlob</code> that is used in the Blob.Attach call
     *
     *  @param fileName Name that is used to save the file as
     *  @param content Base64 encoded content
     */
    public StringBlob(String fileName, String content) {
        super(fileName, null);
        this.content = content;
    }

    /**
     * Creates a <code>StringBlob</code> that is used in the Blob.Attach call
     *
     * @param fileName Name that is used to save the file as
     * @param content Base64 encoded content
     * @param mimeType Mime type to use for this content
     *
     */
    public StringBlob(String fileName, String content, String mimeType) {
        super(fileName, mimeType);
        this.content = content;
    }

    /**
     * Set the charset to be used when to transform the content into a byte stream.
     * If npt specified the default charset will be used.
     * @param charset
     */
    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getCharset() {
        return charset;
    }

    @Override
    public InputStream getStream() throws IOException {
        byte[] bytes = charset == null ? content.getBytes() : content.getBytes(charset);
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public String toString() {
        return content;
    }

}
