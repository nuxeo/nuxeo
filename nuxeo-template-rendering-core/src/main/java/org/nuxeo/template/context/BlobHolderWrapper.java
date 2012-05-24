package org.nuxeo.template.context;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.runtime.api.Framework;

/**
 * Class helper used to expose Document as a {@link BlobHolder} in FreeMarker
 * context
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
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
        try {
            return bh.getBlob();
        } catch (ClientException e) {
            log.error("Unable to retrieve Blob");
            return null;
        }
    }

    public List<Blob> getBlobs() {
        if (bh == null) {
            return null;
        }
        try {
            return bh.getBlobs();
        } catch (ClientException e) {
            log.error("Unable to retrieve Blobs");
            return null;
        }
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

        StringBuffer sb = new StringBuffer(getContextPathProperty());
        sb.append("/nxbigfile/");
        sb.append(doc.getRepositoryName());
        sb.append("/");
        sb.append(doc.getId());
        sb.append("/blobholder:");
        sb.append(index);
        sb.append("/");
        sb.append(blobs.get(index).getFilename());
        sb.append("?inline=true");
        return sb.toString();
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
