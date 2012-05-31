/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.model.impl;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.io.ExportConstants;

/**
 * Implementation of a content property (blob), defined by the {@code content}
 * low level complex type.
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

    public ContentProperty(String encoding, String mimeType, String filename,
            String digest) {
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

    public void setSubProperty(String subPropertyName, String subPropertyValue)
            throws ClientException {
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
            throw new ClientException(
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

        if (encoding == null && otherEncoding == null && mimeType == null
                && otherMimeType == null && filename == null
                && otherFilename == null && digest == null
                && otherDigest == null) {
            return true;
        }

        if (notEquals(encoding, otherEncoding)
                || notEquals(mimeType, otherMimeType)
                || notEquals(filename, otherFilename)
                || notEquals(digest, otherDigest)) {
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
        return s1 == null && s2 != null || s1 != null && s2 == null
                || s1 != null && !s1.equals(s2);
    }
}
