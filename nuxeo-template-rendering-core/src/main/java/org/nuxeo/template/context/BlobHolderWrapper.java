/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.template.context;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.runtime.api.Framework;

/**
 * Class helper used to expose Document as a {@link BlobHolder} in FreeMarker context
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class BlobHolderWrapper {

    protected final BlobHolder bh;

    protected final DocumentModel doc;

    protected static final Log log = LogFactory.getLog(BlobHolderWrapper.class);

    public BlobHolderWrapper(DocumentModel doc) {
        bh = doc.getAdapter(BlobHolder.class);
        this.doc = doc;
    }

    protected static String getContextPathProperty() {
        return Framework.getProperty("org.nuxeo.ecm.contextPath", "/nuxeo");
    }

    public Blob getBlob() {
        if (bh == null) {
            return null;
        }
        return bh.getBlob();
    }

    public List<Blob> getBlobs() {
        if (bh == null) {
            return null;
        }
        return bh.getBlobs();
    }

    public Blob getBlob(String name) {

        List<Blob> blobs = getBlobs();
        if (blobs == null) {
            return null;
        }
        for (Blob blob : blobs) {
            if (name.equals(blob.getFilename())) {
                return blob;
            }
        }
        return null;
    }

    public Blob getBlob(int index) {

        List<Blob> blobs = getBlobs();
        if (blobs == null) {
            return null;
        }
        if (index >= blobs.size()) {
            return null;
        }
        return blobs.get(index);
    }

    public String getBlobUrl(int index) {

        List<Blob> blobs = getBlobs();
        if (blobs == null) {
            return null;
        }
        if (index >= blobs.size()) {
            return null;
        }

        DownloadService downloadService = Framework.getService(DownloadService.class);
        String xpath = DownloadService.BLOBHOLDER_PREFIX + index;
        String filename = blobs.get(index).getFilename();
        return getContextPathProperty() + "/" + downloadService.getDownloadUrl(doc, xpath, filename) + "?inline=true";
    }

    public String getBlobUrl(String name) {

        List<Blob> blobs = getBlobs();
        if (blobs == null) {
            return null;
        }
        for (int index = 0; index < blobs.size(); index++) {
            if (name.equals(blobs.get(index).getFilename())) {
                return getBlobUrl(index);
            }
        }
        return null;
    }

}
