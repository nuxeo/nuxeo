/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    @Override
    public abstract Blob getBlob() throws ClientException;

    @Override
    public void setBlob(Blob blob) throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
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

    @Override
    public String getFilePath() throws ClientException {
        String path = getBasePath();

        Blob blob = getBlob();
        if (blob != null) {
            path = path + "/" + blob.getFilename();
        }

        return path;
    }

    @Override
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

    @Override
    public abstract Calendar getModificationDate() throws ClientException;

}
