/*
 * (C) Copyright 2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.transientstore;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.transientstore.api.StorageEntry;

/**
 * Base class for {@link StorageEntry} implementation
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
public abstract class AbstractStorageEntry implements StorageEntry {

    private static final long serialVersionUID = 1L;

    protected final String id;

    protected boolean hasBlobs = false;

    protected Map<String, Serializable> params;

    protected transient List<Blob> blobs;

    protected List<Map<String, String>> cachedBlobs;

    protected AbstractStorageEntry(String id) {
        this.id=id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setBlobs(List<Blob> blobs) {
        this.blobs=blobs;
        hasBlobs = blobs!=null;
    }

    @Override
    public List<Blob> addBlob(Blob blob)  {
        if (blobs == null) {
            blobs = new ArrayList<Blob>();
        }
        blobs.add(blob);
        hasBlobs=true;
        return blobs;
    }

    @Override
    public List<Blob> getBlobs() {
        return blobs;
    }

    @Override
    public void put(String key, Serializable value) {
        if (params==null) {
            params = new HashMap<String, Serializable>();
        }
        params.put(key, value);
    }

    @Override
    public Serializable get(String key) {
        if (params!=null) {
            return params.get(key);
        }
        return null;
    }

    @Override
    public void persist(File directory) throws IOException {
        if (hasBlobs) {
            cachedBlobs = new ArrayList<Map<String,String>>();
            for (Blob blob : blobs) {
                Map<String, String> cached = new HashMap<String, String>();
                File cachedFile = new File (directory,blob.getFilename());
                blob.transferTo(cachedFile);
                cachedFile.deleteOnExit();
                cached.put("file", cachedFile.getAbsolutePath());
                cached.put("filename", blob.getFilename());
                cached.put("encoding", blob.getEncoding());
                cached.put("mimetype", blob.getMimeType());
                cachedBlobs.add(cached);
            }
            blobs = null;
        }
    }

    @Override
    public void load(File directory) {
        if (!hasBlobs || blobs!=null) {
            return;
        }
        blobs = new ArrayList<Blob>();
        for (Map<String, String> info : cachedBlobs) {
            File cachedFile = new File (info.get("file"));
            Blob blob = new FileBlob(cachedFile);
            blob.setEncoding(info.get("encoding"));
            blob.setMimeType(info.get("mimetype"));
            blob.setFilename(info.get("filename"));
            blobs.add(blob);
        }
        cachedBlobs = null;
    }

    @Override
    public void update(StorageEntry other) {
     // XXX not sure about the semantic
    }

    @Override
    public long getSize() {
        int size = 0;
        for (Blob blob : blobs) {
            size+= blob.getLength();
        }
        return size;
    }
}
