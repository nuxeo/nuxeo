package org.nuxeo.ecm.quota.size;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_REMOVE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_MOVED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED_BY_COPY;
import static org.nuxeo.ecm.quota.size.QuotaAwareDocument.DOCUMENTS_SIZE_STATISTICS_FACET;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.ShallowDocumentModel;

public class QuotaComputerProcessor implements PostCommitEventListener {

    protected static final Log log = LogFactory.getLog(QuotaComputerProcessor.class);

    @Override
    public void handleEvent(EventBundle eventBundle) throws ClientException {

        if (eventBundle.containsEventName(SizeUpdateEventContext.QUOTA_UPDATE_NEEDED)) {

            for (Event event : eventBundle) {
                if (event.getName().equals(
                        SizeUpdateEventContext.QUOTA_UPDATE_NEEDED)) {
                    EventContext ctx = event.getContext();

                    if (ctx instanceof DocumentEventContext) {
                        if (log.isTraceEnabled()) {
                            String sid = ((DocumentEventContext) ctx).getCoreSession().getSessionId();
                            log.trace("Orginal SessionId:" + sid);
                        }
                        SizeUpdateEventContext quotaCtx = SizeUpdateEventContext.unwrap((DocumentEventContext) ctx);
                        if (quotaCtx != null) {
                            processQuotaComputation(quotaCtx);
                            // double check
                            debugCheck(quotaCtx);
                        }
                    }
                }
            }
        }
    }

    protected void debugCheck(SizeUpdateEventContext quotaCtx)
            throws ClientException {
        String sourceEvent = quotaCtx.getSourceEvent();
        CoreSession session = quotaCtx.getCoreSession();
        DocumentModel sourceDocument = quotaCtx.getSourceDocument();

        if (session.exists(sourceDocument.getRef())) {
            DocumentModel doc = session.getDocument(sourceDocument.getRef());
            if (log.isTraceEnabled()) {
                if (doc.hasFacet(DOCUMENTS_SIZE_STATISTICS_FACET)) {
                    log.trace("Double Check Facet was added OK");
                } else {
                    log.trace("No facet !!!!");
                }
            }
        } else {
            log.debug("Document " + sourceDocument.getRef()
                    + " no longer exists (" + sourceEvent + ")");
        }

    }

    public void processQuotaComputation(SizeUpdateEventContext quotaCtx)
            throws ClientException {
        String sourceEvent = quotaCtx.getSourceEvent();
        CoreSession session = quotaCtx.getCoreSession();
        DocumentModel sourceDocument = quotaCtx.getSourceDocument();

        if (sourceDocument instanceof ShallowDocumentModel) {
            if (!ABOUT_TO_REMOVE.equals(sourceEvent)) {
                log.error("Unable to reconnect Document "
                        + sourceDocument.getPathAsString() + " on event "
                        + sourceEvent);
                return;
            }
        } else {
            if (log.isTraceEnabled()) {
                log.trace("sourceDoc SessionId:"
                        + sourceDocument.getSessionId());
                log.trace("sourceDoc SessionId:"
                        + sourceDocument.getCoreSession().getSessionId());
            }
        }

        List<DocumentModel> parents = new ArrayList<DocumentModel>();

        log.debug("compute Quota on " + sourceDocument.getPathAsString()
                + " and parents");

        if (ABOUT_TO_REMOVE.equals(sourceEvent)) {
            // use the store list of parentIds
            for (String id : quotaCtx.getParentUUIds()) {
                if (session.exists(new IdRef(id))) {
                    parents.add(session.getDocument(new IdRef(id)));
                }
            }
        } else if (DOCUMENT_MOVED.equals(sourceEvent)) {

            if (quotaCtx.getParentUUIds() != null
                    && quotaCtx.getParentUUIds().size() > 0) {
                // use the store list of parentIds
                for (String id : quotaCtx.getParentUUIds()) {
                    if (session.exists(new IdRef(id))) {
                        parents.add(session.getDocument(new IdRef(id)));
                    }
                }
            } else {
                parents.addAll(session.getParentDocuments(sourceDocument.getRef()));
                Collections.reverse(parents);
                parents.remove(0);
            }
        } else {

            if (sourceDocument.getRef() == null) {
                log.error("SourceDocument has no ref");
            } else {
                parents.addAll(session.getParentDocuments(sourceDocument.getRef()));
                Collections.reverse(parents);
                parents.remove(0);
            }

            // process Quota on target Document
            if (!DOCUMENT_CREATED_BY_COPY.equals(sourceEvent)) {
                QuotaAware quotaDoc = sourceDocument.getAdapter(QuotaAware.class);
                if (quotaDoc == null) {
                    log.debug("  add Quota Facet on "
                            + sourceDocument.getPathAsString());
                    quotaDoc = QuotaAwareDocumentFactory.make(sourceDocument,
                            false);

                } else {
                    log.debug("  update Quota Facet on "
                            + sourceDocument.getPathAsString());
                }
                quotaDoc.addInnerSize(quotaCtx.getBlobDelta(), true);
            }
        }
        if (parents.size() > 0) {
            processOnParents(parents, quotaCtx.getBlobDelta());
        }
    }

    protected void processOnParents(List<DocumentModel> parents, long delta)
            throws ClientException {
        for (DocumentModel parent : parents) {
            if (parent.getPathAsString().equals("/")) {
                continue;
            }
            // process Quota on target Document
            QuotaAware quotaDoc = parent.getAdapter(QuotaAware.class);
            if (quotaDoc == null) {
                log.debug("   add Quota Facet on parent "
                        + parent.getPathAsString());
                quotaDoc = QuotaAwareDocumentFactory.make(parent, false);
                quotaDoc.getDoc().setPropertyValue("dc:nature", "samere");
            } else {
                log.debug("   update Quota Facet on parent "
                        + parent.getPathAsString());
            }
            quotaDoc.addTotalSize(delta, true);
        }
    }
}
