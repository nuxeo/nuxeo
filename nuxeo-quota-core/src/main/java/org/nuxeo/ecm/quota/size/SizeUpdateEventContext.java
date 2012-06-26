package org.nuxeo.ecm.quota.size;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

public class SizeUpdateEventContext extends DocumentEventContext {

    public static final String QUOTA_UPDATE_NEEDED = "quotaUpdateNeeded";

    private static final long serialVersionUID = 1L;

    public static final String BLOB_SIZE_PROPERTY_KEY = "blobSize";

    public static final String BLOB_DELTA_PROPERTY_KEY = "blobDelta";

    public static final String PARENT_UUIDS_PROPERTY_KEY = "parentUUIDs";

    public static final String SOURCE_EVENT_PROPERTY_KEY = "sourceEvent";

    public static final String MARKER_KEY = "contextType";

    public static final String MARKER_VALUE = "SizeUpdateEventContext";

    protected SizeUpdateEventContext(DocumentEventContext evtCtx) {
        super(evtCtx.getCoreSession(), evtCtx.getPrincipal(),
                evtCtx.getSourceDocument(), evtCtx.getDestination());
        setProperty(MARKER_KEY, MARKER_VALUE);
    }

    public SizeUpdateEventContext(DocumentEventContext evtCtx,
            DocumentModel sourceDocument, BlobSizeInfo bsi, String sourceEvent) {
        super(evtCtx.getCoreSession(), evtCtx.getPrincipal(), sourceDocument,
                evtCtx.getDestination());
        setBlobSize(bsi.getBlobSize());
        setBlobDelta(bsi.getBlobSizeDelta());
        setProperty(SOURCE_EVENT_PROPERTY_KEY, sourceEvent);
        setProperty(MARKER_KEY, MARKER_VALUE);
    }

    public SizeUpdateEventContext(DocumentEventContext evtCtx,
            BlobSizeInfo bsi, String sourceEvent) {
        super(evtCtx.getCoreSession(), evtCtx.getPrincipal(),
                evtCtx.getSourceDocument(), evtCtx.getDestination());
        setBlobSize(bsi.getBlobSize());
        setBlobDelta(bsi.getBlobSizeDelta());
        setProperty(SOURCE_EVENT_PROPERTY_KEY, sourceEvent);
        setProperty(MARKER_KEY, MARKER_VALUE);
    }

    public SizeUpdateEventContext(DocumentEventContext evtCtx, long totalSize,
            String sourceEvent) {
        super(evtCtx.getCoreSession(), evtCtx.getPrincipal(),
                evtCtx.getSourceDocument(), evtCtx.getDestination());
        setBlobSize(totalSize);
        setBlobDelta(-totalSize);
        setProperty(SOURCE_EVENT_PROPERTY_KEY, sourceEvent);
        setProperty(MARKER_KEY, MARKER_VALUE);
    }

    public static SizeUpdateEventContext unwrap(DocumentEventContext docCtx) {
        if (MARKER_VALUE.equals(docCtx.getProperty(MARKER_KEY))) {
            SizeUpdateEventContext ctx = new SizeUpdateEventContext(docCtx);
            ctx.setProperties(docCtx.getProperties());
            return ctx;
        }
        return null;
    }

    public long getBlobSize() {
        return (Long) getProperty(BLOB_SIZE_PROPERTY_KEY);
    }

    public void setBlobSize(long blobSize) {
        setProperty(BLOB_SIZE_PROPERTY_KEY, new Long(blobSize));
    }

    public long getBlobDelta() {
        return (Long) getProperty(BLOB_DELTA_PROPERTY_KEY);
    }

    public void setBlobDelta(long blobDelta) {
        setProperty(BLOB_DELTA_PROPERTY_KEY, new Long(blobDelta));
    }

    @SuppressWarnings("unchecked")
    public List<String> getParentUUIds() {
        return (List<String>) getProperty(PARENT_UUIDS_PROPERTY_KEY);
    }

    public void setParentUUIds(List<String> parentUUIds) {
        setProperty(PARENT_UUIDS_PROPERTY_KEY, (Serializable) parentUUIds);
    }

    public String getSourceEvent() {
        return (String) getProperty(SOURCE_EVENT_PROPERTY_KEY);
    }

}
