/*
 * (C) Copyright 2002-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.api.blobholder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * Base class for {@link BlobHolder} implementers
 */
public abstract class AbstractBlobHolder implements BlobHolder {

    public abstract Blob getBlob() throws ClientException;

    public void setBlob(Blob blob) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public List<Blob> getBlobs() throws ClientException {
        List<Blob> blobs = null;

        Blob blob = getBlob();
        if (blob != null) {
            blobs = new ArrayList<Blob>();
            blobs.add(blob);
        }

        return blobs;
    }

    protected abstract String getBasePath();

    public String getFilePath() throws ClientException {
        String path = getBasePath();

        Blob blob = getBlob();
        if (blob != null) {
            path = path + "/" + blob.getFilename();
        }

        return path;
    }

    public String getHash() throws ClientException {

        Blob blob = getBlob();
        if (blob != null) {
            String h = blob.getDigest();
            if (h != null) {
                return h;
            } else {
                return Integer.toString(blob.hashCode());
            }
        }
        return "NullBlob";
    }

    public abstract Calendar getModificationDate() throws ClientException;

}
