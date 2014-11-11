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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.api.blobholder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * {@link BlobHolder} implementation that simply wraps a detached {@link Blob}.
 *
 * @author tiry
 */
public class SimpleBlobHolder extends AbstractBlobHolder {

    protected List<Blob> blobs;

    protected Calendar creationDate;

    public SimpleBlobHolder() {
        // Empty constructor
    }

    public SimpleBlobHolder(List<Blob> blobs) {
        init(blobs);
    }

    public SimpleBlobHolder(Blob blob) {
        blobs = new ArrayList<Blob>();
        blobs.add(blob);
        init(blobs);
    }

    protected void init(List<Blob> blobs) {
        this.blobs = blobs;
        creationDate = Calendar.getInstance();
    }

    @Override
    public Blob getBlob() throws ClientException {
        if (blobs == null || blobs.size() == 0) {
            return null;
        } else {
            return blobs.get(0);
        }
    }

    @Override
    public List<Blob> getBlobs() throws ClientException {
        return blobs;
    }

    @Override
    protected String getBasePath() {
        return "";
    }

    @Override
    public Calendar getModificationDate() throws ClientException {
        return creationDate;
    }

    @Override
    public Serializable getProperty(String name) throws ClientException {
        return null;
    }

    @Override
    public Map<String, Serializable> getProperties() {
        return null;
    }

}
