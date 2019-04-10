/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.model.impl;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.io.ExportConstants;

/**
 * Implementation of a content property (blob), defined by the {@code content} low level complex type.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public class ContentProperty implements Serializable {

    private static final long serialVersionUID = 4258464052505119020L;

    protected String encoding;

    protected String mimeType;

    protected String filename;

    protected String digest;

    public ContentProperty() {
    }

    public ContentProperty(String encoding, String mimeType, String filename, String digest) {
        this.encoding = encoding;
        this.mimeType = mimeType;
        this.filename = filename;
        this.digest = digest;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public void setSubProperty(String subPropertyName, String subPropertyValue) {
        if (ExportConstants.BLOB_ENCODING.equals(subPropertyName)) {
            setEncoding(subPropertyValue);
        } else if (ExportConstants.BLOB_MIME_TYPE.equals(subPropertyName)) {
            setMimeType(subPropertyValue);
        } else if (ExportConstants.BLOB_FILENAME.equals(subPropertyName)) {
            setFilename(subPropertyValue);
        } else if (ExportConstants.BLOB_DIGEST.equals(subPropertyName)) {
            setDigest(subPropertyValue);
        } else if (ExportConstants.BLOB_DATA.equals(subPropertyName)) {
            // Nothing to do here, the data property is not interesting for the
            // content diff
        } else {
            throw new NuxeoException(
                    String.format(
                            "Error while trying to set sub property '%s' on an object of type ContentProperty: no such sub property.",
                            subPropertyName));
        }
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }
        if (!(other instanceof ContentProperty)) {
            return false;
        }

        String otherEncoding = ((ContentProperty) other).getEncoding();
        String otherMimeType = ((ContentProperty) other).getMimeType();
        String otherFilename = ((ContentProperty) other).getFilename();
        String otherDigest = ((ContentProperty) other).getDigest();

        if (encoding == null && otherEncoding == null && mimeType == null && otherMimeType == null && filename == null
                && otherFilename == null && digest == null && otherDigest == null) {
            return true;
        }

        if (notEquals(encoding, otherEncoding) || notEquals(mimeType, otherMimeType)
                || notEquals(filename, otherFilename) || notEquals(digest, otherDigest)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("filename=");
        sb.append(filename);
        sb.append("; encoding=");
        sb.append(encoding);
        sb.append("; mimeType=");
        sb.append(mimeType);
        sb.append("; digest=");
        sb.append(digest);
        return sb.toString();
    }

    protected boolean notEquals(String s1, String s2) {
        return s1 == null && s2 != null || s1 != null && s2 == null || s1 != null && !s1.equals(s2);
    }
}
