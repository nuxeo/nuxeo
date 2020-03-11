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
 *     Thierry Delprat
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.api.blobholder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Base class for {@link BlobHolder} implementers
 */
public abstract class AbstractBlobHolder implements BlobHolder {

    @Override
    public abstract Blob getBlob();

    @Override
    public void setBlob(Blob blob) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Blob> getBlobs() {
        List<Blob> blobs = null;

        Blob blob = getBlob();
        if (blob != null) {
            blobs = new ArrayList<>();
            blobs.add(blob);
        }

        return blobs;
    }

    protected abstract String getBasePath();

    @Override
    public String getFilePath() {
        String path = getBasePath();

        Blob blob = getBlob();
        if (blob != null) {
            path = path + "/" + blob.getFilename();
        }

        return path;
    }

    @Override
    public String getHash() {

        Blob blob = getBlob();
        if (blob != null) {
            String h = blob.getDigest();
            if (h == null) {
                h = getMD5Digest();
                blob.setDigest(h);
            }
            return h;
        }
        return "NullBlob";
    }

    protected String getMD5Digest() {
        try (InputStream in = getBlob().getStream()) {
            return DigestUtils.md5Hex(in);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public abstract Calendar getModificationDate();

}
