package org.nuxeo.template.listeners;

import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

public class TemplateDeletionGuard implements EventListener {

    @Override
    public void handleEvent(Event event) throws ClientException {

        EventContext ctx = event.getContext();

        if (DocumentEventTypes.ABOUT_TO_REMOVE.equals(event.getName())) {
            if (ctx instanceof DocumentEventContext) {
                DocumentEventContext docCtx = (DocumentEventContext) ctx;
                DocumentModel targetDoc = docCtx.getSourceDocument();

                TemplateSourceDocument templateDoc = targetDoc.getAdapter(TemplateSourceDocument.class);
                if (templateDoc != null && !Framework.isTestModeSet()) {
                    if (templateDoc.getTemplateBasedDocuments().size() > 0) {
                        TransactionHelper.setTransactionRollbackOnly();
                        event.cancel();
                        // XXX should do better
                        FacesMessages.instance().clearGlobalMessages();
                        FacesMessages.instance().addFromResourceBundleOrDefault(StatusMessage.Severity.WARN,
                                "label.template.canNotDeletedATemplateInUse",
                                "Can not delete a template that is still in use.");
                    }
                }
            }
        }
    }
}
