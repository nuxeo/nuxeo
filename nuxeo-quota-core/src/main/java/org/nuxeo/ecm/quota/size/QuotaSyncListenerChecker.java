package org.nuxeo.ecm.quota.size;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_REMOVE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED_BY_COPY;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_MOVED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.utils.BlobsExtractor;
import org.nuxeo.runtime.api.Framework;

public class QuotaSyncListenerChecker implements EventListener {

    public static final List<String> EVENTS_TO_HANDLE = Arrays.asList(
            ABOUT_TO_REMOVE, DOCUMENT_CREATED_BY_COPY, DOCUMENT_MOVED,
            BEFORE_DOC_UPDATE, DOCUMENT_CREATED);

    public static final String DISABLE_QUOTA_CHECK_LISTENER = "disableQuotaListener";

    protected static final Log log = LogFactory.getLog(QuotaSyncListenerChecker.class);

    @Override
    public void handleEvent(Event event) throws ClientException {

        try {
            if (EVENTS_TO_HANDLE.contains(event.getName())) {
                EventContext ctx = event.getContext();
                if (ctx instanceof DocumentEventContext) {
                    DocumentEventContext docCtx = (DocumentEventContext) ctx;
                    DocumentModel targetDoc = docCtx.getSourceDocument();
                    CoreSession session = docCtx.getCoreSession();

                    if (!needToProcessEventOnDocument(event, targetDoc)) {
                        return;
                    }

                    log.debug("Preprocess Document "
                            + targetDoc.getPathAsString() + " on event "
                            + event.getName());

                    if (DOCUMENT_CREATED.equals(event.getName())) {
                        processDocumentCreated(session, targetDoc, docCtx);
                    } else if (BEFORE_DOC_UPDATE.equals(event.getName())) {
                        processDocumentBeforeUpdate(session, targetDoc, docCtx);
                    } else if (ABOUT_TO_REMOVE.equals(event.getName())) {
                        processDocumentAboutToBeRemoved(session, targetDoc,
                                docCtx);
                    } else if (DOCUMENT_CREATED_BY_COPY.equals(event.getName())) {
                        processDocumentCopied(session, targetDoc, docCtx);
                    } else if (DOCUMENT_MOVED.equals(event.getName())) {
                        DocumentRef sourceParentRef = (DocumentRef) docCtx.getProperty(CoreEventConstants.PARENT_PATH);
                        DocumentModel sourceParent = session.getDocument(sourceParentRef);
                        processDocumentMoved(session, targetDoc, sourceParent,
                                docCtx);
                    }
                }
            }
        } catch (ClientException e) {
            ClientException e2 = handleException(e, event);
            if (e2 != null) {
                throw e2;
            }
        }
    }

    protected ClientException handleException(ClientException e, Event event) {
        if (e instanceof QuotaExceededException) {
            event.markRollBack("Quota Exceeded", e);
        }
        return e;
    }

    protected void processDocumentCreated(CoreSession session,
            DocumentModel targetDoc, DocumentEventContext docCtx)
            throws ClientException {
        BlobSizeInfo bsi = computeSizeImpact(targetDoc, false);
        // only process if Blobs where added or removed
        if (bsi.getBlobSizeDelta() != 0) {
            checkConstraints(targetDoc, targetDoc.getParentRef(), bsi);
            SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(
                    docCtx, bsi, DOCUMENT_CREATED);
            sendUpdateEvents(asyncEventCtx);
        }

    }

    protected void processDocumentUpdated(CoreSession session,
            DocumentModel doc, DocumentEventContext docCtx)
            throws ClientException {
        // Nothing to do !
    }

    protected void processDocumentBeforeUpdate(CoreSession session,
            DocumentModel targetDoc, DocumentEventContext docCtx)
            throws ClientException {
        BlobSizeInfo bsi = computeSizeImpact(targetDoc, true);
        // only process if Blobs where added or removed
        if (bsi.getBlobSizeDelta() != 0) {
            checkConstraints(targetDoc, targetDoc.getParentRef(), bsi);
            SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(
                    docCtx, bsi, BEFORE_DOC_UPDATE);
            sendUpdateEvents(asyncEventCtx);
        }
    }

    protected void processDocumentCopied(CoreSession session,
            DocumentModel targetDoc, DocumentEventContext docCtx)
            throws ClientException {
        QuotaAware quotaDoc = targetDoc.getAdapter(QuotaAware.class);
        if (quotaDoc != null) {
            long total = quotaDoc.getTotalSize();
            BlobSizeInfo bsi = new BlobSizeInfo();
            bsi.blobSize = total;
            bsi.blobSizeDelta = total;
            if (total > 0) {
                // check on parent since Session is not
                // committed
                // for now
                checkConstraints(targetDoc, targetDoc.getParentRef(), bsi);
                SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(
                        docCtx, bsi, DOCUMENT_CREATED_BY_COPY);
                sendUpdateEvents(asyncEventCtx);
            }
        }
    }

    protected void processDocumentMoved(CoreSession session,
            DocumentModel targetDoc, DocumentModel sourceParent,
            DocumentEventContext docCtx) throws ClientException {

        QuotaAware quotaDoc = targetDoc.getAdapter(QuotaAware.class);
        long total = 0;
        if (quotaDoc != null) {
            total = quotaDoc.getTotalSize();
        }
        BlobSizeInfo bsi = new BlobSizeInfo();
        bsi.blobSize = total;
        bsi.blobSizeDelta = total;
        if (total > 0) {
            // check on destination parent since Session is not
            // committed
            // for now
            checkConstraints(targetDoc, targetDoc.getParentRef(), bsi);
            SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(
                    docCtx, bsi, DOCUMENT_MOVED);
            sendUpdateEvents(asyncEventCtx);

            // also need to trigger update on source tree
            BlobSizeInfo bsiRemove = new BlobSizeInfo();
            bsiRemove.blobSize = total;
            bsiRemove.blobSizeDelta = -total;

            asyncEventCtx = new SizeUpdateEventContext(docCtx, sourceParent,
                    bsiRemove, DOCUMENT_MOVED);
            List<String> sourceParentUUIDs = getParentUUIDS(sourceParent);
            sourceParentUUIDs.add(0, sourceParent.getId());
            asyncEventCtx.setParentUUIds(sourceParentUUIDs);
            sendUpdateEvents(asyncEventCtx);
        }

    }

    protected void processDocumentAboutToBeRemoved(CoreSession session,
            DocumentModel targetDoc, DocumentEventContext docCtx)
            throws ClientException {

        QuotaAware quotaDoc = targetDoc.getAdapter(QuotaAware.class);
        if (quotaDoc != null) {
            long total = quotaDoc.getTotalSize();
            if (total > 0) {
                List<String> parentUUIDs = getParentUUIDS(targetDoc);
                SizeUpdateEventContext asyncEventCtx = new SizeUpdateEventContext(
                        docCtx, total, ABOUT_TO_REMOVE);
                asyncEventCtx.setParentUUIds(parentUUIDs);
                sendUpdateEvents(asyncEventCtx);
            }
        }
    }

    protected boolean needToProcessEventOnDocument(Event event,
            DocumentModel targetDoc) {

        if (targetDoc == null) {
            return false;
        }
        if (targetDoc.isVersion() || targetDoc.isProxy()) {
            log.debug("Escape from listener : not precessing versions");
            return false;
        }

        Boolean block = (Boolean) targetDoc.getContextData().getScopedValue(
                ScopeType.REQUEST, DISABLE_QUOTA_CHECK_LISTENER);
        if (block != null && block) {
            log.debug("Escape from listener to avoid re-entrency");
            // ignore the event - we are blocked by the caller
            // used to avoid reentrency when the async event handler
            // do
            // update
            // the docs to set the new size !
            return false;
        }
        return true;
    }

    protected void sendUpdateEvents(SizeUpdateEventContext eventCtx)
            throws ClientException {

        Event quotaUpdateEvent = eventCtx.newQuotaUpdateEvent();
        log.debug("prepared event on target tree with context "
                + eventCtx.toString());
        EventService es = Framework.getLocalService(EventService.class);
        es.fireEvent(quotaUpdateEvent);
    }

    protected List<String> getParentUUIDS(final DocumentModel doc)
            throws ClientException {

        final List<String> result = new ArrayList<String>();
        UnrestrictedSessionRunner runner = new UnrestrictedSessionRunner(
                doc.getCoreSession()) {
            @Override
            public void run() throws ClientException {
                DocumentRef[] parentRefs = session.getParentDocumentRefs(doc.getRef());
                for (DocumentRef parentRef : parentRefs) {
                    result.add(parentRef.toString());
                }
            }
        };
        runner.runUnrestricted();
        return result;
    }

    protected void checkConstraints(final DocumentModel doc,
            final DocumentRef parentRef, final BlobSizeInfo bsi)
            throws ClientException {

        if (bsi.blobSizeDelta <= 0) {
            return;
        }

        UnrestrictedSessionRunner runner = new UnrestrictedSessionRunner(
                doc.getCoreSession()) {
            @Override
            public void run() throws ClientException {
                List<DocumentModel> parents = session.getParentDocuments(parentRef);
                parents.add(session.getDocument(parentRef));
                for (DocumentModel parent : parents) {
                    QuotaAware qap = parent.getAdapter(QuotaAware.class);
                    if (qap != null && qap.getMaxQuota() > 0) {
                        if (qap.getTotalSize() + bsi.getBlobSizeDelta() > qap.getMaxQuota()) {
                            log.error("Raising Quota Exception on "
                                    + doc.getPathAsString());
                            throw new QuotaExceededException(parent, doc,
                                    qap.getMaxQuota());
                        }
                    }
                }
            }
        };
        try {
            runner.runUnrestricted();
        } catch (QuotaExceededException e) {
            log.error("Quota Constraints exception", e);
            throw e;
        }

    }

    protected BlobSizeInfo computeSizeImpact(DocumentModel doc,
            boolean onlyChanges) throws ClientException {

        BlobSizeInfo result = new BlobSizeInfo();

        QuotaAware quotaDoc = doc.getAdapter(QuotaAware.class);
        if (quotaDoc != null) {
            result.blobSize = quotaDoc.getInnerSize();
        } else {
            result.blobSize = 0;
        }

        List<Blob> blobs = getBlobs(doc, onlyChanges);

        if (onlyChanges) {
            if (blobs.size() == 0) {
                result.blobSizeDelta = 0;
            } else {
                long size = 0;
                for (Blob blob : blobs) {
                    if (blob != null) {
                        size += blob.getLength();
                    }
                }
                result.blobSizeDelta = size - result.blobSize;
                result.blobSize = size;
            }
        } else {
            if (blobs.size() == 0) {
                result.blobSizeDelta = -result.blobSize;
                result.blobSize = 0;
            } else {
                long size = 0;
                for (Blob blob : blobs) {
                    if (blob != null) {
                        size += blob.getLength();
                    }
                }
                result.blobSizeDelta = size - result.blobSize;
                result.blobSize = size;
            }
        }
        return result;
    }

    protected List<Blob> getBlobs(DocumentModel doc, boolean onlyChangedBlob)
            throws ClientException {

        try {
            BlobsExtractor extractor = new BlobsExtractor();
            List<Property> blobProperties = extractor.getBlobsProperties(doc);

            boolean needRecompute = !onlyChangedBlob;
            if (!needRecompute) {
                if (blobProperties.size() > 0) {
                    for (Property blobProperty : blobProperties) {
                        if (blobProperty.isDirty()) {
                            needRecompute = true;
                            break;
                        }
                    }
                }
            }
            List<Blob> result = new ArrayList<Blob>();
            if (needRecompute) {
                for (Property blobProperty : blobProperties) {
                    Blob blob = (Blob) blobProperty.getValue();
                    result.add(blob);
                }
            }
            return result;
        } catch (Exception e) {
            throw new ClientException("Unable to extract Blob size", e);
        }
    }

}
