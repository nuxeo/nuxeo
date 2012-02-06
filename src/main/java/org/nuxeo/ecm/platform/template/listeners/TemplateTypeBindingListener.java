package org.nuxeo.ecm.platform.template.listeners;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.template.adapters.source.TemplateSourceDocument;
import org.nuxeo.ecm.platform.template.service.TemplateProcessorService;
import org.nuxeo.runtime.api.Framework;



public class TemplateTypeBindingListener implements PostCommitEventListener {

    protected static Log log = LogFactory.getLog(TemplateTypeBindingListener.class);
    
    @Override
    public void handleEvent(EventBundle eventBundle) throws ClientException {
        if (eventBundle.containsEventName(DOCUMENT_CREATED) || eventBundle.containsEventName(DOCUMENT_UPDATED)) {
            for (Event event : eventBundle) {

                if (DOCUMENT_CREATED.equals(event.getName()) || DOCUMENT_UPDATED.equals(event.getName()) )
                {
                    EventContext ctx = event.getContext();
                    if (ctx instanceof DocumentEventContext) {
                        DocumentEventContext docCtx = (DocumentEventContext) ctx;
                        DocumentModel targetDoc = docCtx.getSourceDocument();

                        if (targetDoc.isVersion()) {
                            continue ;
                        }
                        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);
                        try {
                            TemplateSourceDocument tmpl = targetDoc.getAdapter(TemplateSourceDocument.class);
                            if (tmpl!=null) {
                                tps.registerTypeMapping(targetDoc);
                                // be sure to trigger invalidations in unit tests
                                targetDoc.getCoreSession().save();
                            }
                        } catch (Throwable e) {
                            log.warn("Unable to get adapter to check type binding");
                        }
                    }
                }
            }
        }
    }

}
