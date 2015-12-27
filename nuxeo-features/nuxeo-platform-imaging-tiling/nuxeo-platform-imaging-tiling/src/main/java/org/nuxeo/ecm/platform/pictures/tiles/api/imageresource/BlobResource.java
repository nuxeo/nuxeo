/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 *
 */
package org.nuxeo.ecm.platform.pictures.tiles.api.imageresource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.nuxeo.ecm.core.api.Blob;

/**
 * Blob based implementation of the ImageResource Because ImageResource will be cached this Implementation is not
 * optimal (Blob digest is not compulsory and the modification date is not set).
 * <p>
 * This implementation is mainly used for unit testing.
 *
 * @author tiry
 */
public class BlobResource implements ImageResource {

    private static final long serialVersionUID = 1L;

    protected Blob blob;

    protected String hash;

    protected Calendar modified;

    public BlobResource(Blob blob) {
        this.blob = blob;
        if (blob.getDigest() != null) {
            hash = blob.getDigest();
        } else {
            hash = getMD5Digest();
        }

        modified = Calendar.getInstance();
    }

    public Blob getBlob() {
        return blob;
    }

    public String getHash() {
        return hash;
    }

    public Calendar getModificationDate() {
        return modified;
    }

    private String getMD5Digest() {
        try (InputStream in = blob.getStream()) {
            return DigestUtils.md5Hex(in);
        } catch (IOException e) {
            return blob.hashCode() + "fakeHash";
        }
    }

}
