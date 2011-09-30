/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.automation.server.jaxrs.batch;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

/**
 *
 * Batch Object to encapsulate all data related to a batch, especially the
 * temporary files used for Blobs
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.2
 */
public class Batch {

    protected Map<String, Blob> uploadedBlob = new ConcurrentHashMap<String, Blob>();

    protected final String id;

    protected final String baseDir;

    public Batch(String id) {
        this.id = id;
        baseDir = new Path(System.getProperty("java.io.tmpdir")).append(id).toString();
        new File(baseDir).mkdirs();
    }

    public void addBlob(String idx, Blob blob) {
        uploadedBlob.put(idx, blob);
    }

    public void addStream(String idx, InputStream is, String name, String mime)
            throws IOException {

        File tmp = new File(new Path(baseDir).append(name).toString());
        FileUtils.copyToFile(is, tmp);
        FileBlob blob = new FileBlob(tmp);
        if (mime != null) {
            blob.setMimeType(mime);
        } else {
            blob.setMimeType("application/octetstream");
        }
        blob.setFilename(name);
        addBlob(idx, blob);
    }

    /**
     * Return the uploaded blobs in the order the user choose to upload them
     *
     * @return
     */
    public List<Blob> getBlobs() {

        List<Blob> blobs = new ArrayList<Blob>();

        List<String> sortedIdx = new ArrayList<String>(uploadedBlob.keySet());
        Collections.sort(sortedIdx);

        for (String k : sortedIdx) {
            blobs.add(uploadedBlob.get(k));
        }
        return blobs;
    }

    public Blob getBlob(String fileId) {
        return uploadedBlob.get(fileId);
    }

    public void clear() {
        uploadedBlob.clear();
        FileUtils.deleteTree(new File(baseDir));
    }

}
