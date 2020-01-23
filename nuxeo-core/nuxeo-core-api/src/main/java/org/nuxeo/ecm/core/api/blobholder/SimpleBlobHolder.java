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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.api.blobholder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;

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
    public void setBlob(Blob blob) {
        init(new ArrayList<>(Arrays.asList(blob)));
    }

    @Override
    public Blob getBlob() {
        if (blobs == null || blobs.size() == 0) {
            return null;
        } else {
            return blobs.get(0);
        }
    }

    @Override
    public List<Blob> getBlobs() {
        return blobs;
    }

    @Override
    protected String getBasePath() {
        return "";
    }

    @Override
    public Calendar getModificationDate() {
        return creationDate;
    }

    @Override
    public Serializable getProperty(String name) {
        return null;
    }

    @Override
    public Map<String, Serializable> getProperties() {
        return null;
    }

}
