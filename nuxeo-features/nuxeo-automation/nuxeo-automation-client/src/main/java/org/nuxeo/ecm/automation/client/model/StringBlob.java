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

/**
 * An in memory bob containing a string.
 *
 * @author bstefanescu
 */
public class StringBlob extends Blob {

    private static final long serialVersionUID = -7170366401800302228L;

    /** content */
    protected final String content;

    protected String charset;

    public StringBlob(String content) {
        this.content = content;
    }

    /**
     * Creates a <code>StringBlob</code> that is used in the Blob.Attach call
     *
     * @param fileName Name that is used to save the file as
     * @param content Base64 encoded content
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
     */
    public StringBlob(String fileName, String content, String mimeType) {
        super(fileName, mimeType);
        this.content = content;
    }

    /**
     * Set the charset to be used when to transform the content into a byte stream. If npt specified the default charset
     * will be used.
     *
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

    @Override
    public int getLength() {
        return content.length();
    }
}
