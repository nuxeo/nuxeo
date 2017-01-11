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
import java.util.concurrent.atomic.AtomicInteger;

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

    protected final AtomicInteger uploadInProgress = new AtomicInteger(0);

    public Batch(String id) {
        this.id = id;

        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        baseDir = new Path(tmpDir.getPath()).append(id).toString();

        File file = new File(baseDir);

        try {
            if (!file.getCanonicalPath().startsWith(tmpDir.getCanonicalPath())) {
                throw new SecurityException("Trying to traverse illegal path");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error when trying to create Batch", e);
        }

        file.mkdirs();
    }

    public void addBlob(String idx, Blob blob) {
        uploadedBlob.put(idx, blob);
    }

    public void addStream(String idx, InputStream is, String name, String mime)
            throws IOException {

        uploadInProgress.incrementAndGet();
        try {
            File tmp = new File(new Path(baseDir).append(String.valueOf(System.nanoTime())).toString());
            FileUtils.copyToFile(is, tmp);
            FileBlob blob = new FileBlob(tmp);
            if (mime != null) {
                blob.setMimeType(mime);
            } else {
                blob.setMimeType("application/octet-stream");
            }
            blob.setFilename(name);
            addBlob(idx, blob);
        } finally {
            uploadInProgress.decrementAndGet();
        }
    }

    /**
     * Return the uploaded blobs in the order the user choose to upload them
     *
     * @return
     */
    public List<Blob> getBlobs() {
        return getBlobs(0);

    }

    /**
     * @since 5.7
     *
     * @param timeoutS
     * @return
     */
    public List<Blob> getBlobs(int timeoutS) {

        List<Blob> blobs = new ArrayList<Blob>();

        if (uploadInProgress.get() > 0 && timeoutS > 0) {
            for (int i = 0; i < timeoutS * 5; i++) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (uploadInProgress.get() == 0) {
                    break;
                }
            }
        }
        List<String> sortedIdx = new ArrayList<String>(uploadedBlob.keySet());
        Collections.sort(sortedIdx);

        for (String k : sortedIdx) {
            blobs.add(uploadedBlob.get(k));
        }
        return blobs;
    }

    public Blob getBlob(String fileId) {
        return getBlob(fileId, 0);
    }

    /**
     * @since 5.7
     *
     * @param fileId
     * @param timeoutS
     * @return
     */
    public Blob getBlob(String fileId, int timeoutS) {

        Blob result = uploadedBlob.get(fileId);
        if (result == null && timeoutS > 0 && uploadInProgress.get() > 0) {
            for (int i = 0; i < timeoutS * 5; i++) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                result = uploadedBlob.get(fileId);
                if (result != null) {
                    break;
                }
            }
        }
        return result;
    }

    public void clear() {
        uploadedBlob.clear();
        FileUtils.deleteTree(new File(baseDir));
    }

}
