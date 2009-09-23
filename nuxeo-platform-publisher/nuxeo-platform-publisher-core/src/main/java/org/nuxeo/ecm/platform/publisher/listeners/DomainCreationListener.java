package org.nuxeo.ecm.platform.publisher.listeners;

import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.impl.service.PublisherServiceImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Handle Domain creation.
 * Register new {@code PublicationTreeConfigDescriptor} according to the new Domain, if at
 * least one descriptor is pending.
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class DomainCreationListener implements EventListener {

    public void handleEvent(Event event) throws ClientException {
        EventContext ctx = event.getContext();
        if (ctx instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) ctx;
            DocumentModel doc = docCtx.getSourceDocument();
            if (doc.getType().equals("Domain")) {
                try {
                PublisherServiceImpl service = (PublisherServiceImpl) Framework.getService(PublisherService.class);
                service.registerTreeConfigFor(doc);
                } catch (Exception e) {
                    throw new ClientException(e);
                }
            }
        }
    }

}
