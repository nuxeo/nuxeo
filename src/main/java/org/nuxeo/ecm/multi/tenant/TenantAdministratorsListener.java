package org.nuxeo.ecm.multi.tenant;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.usermanager.UserManagerImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.event.EventService;

/**
 * Listeners invalidating the principals cache when the tenant administrators
 * are changed.
 *
 * @since 5.9.2
 */
public class TenantAdministratorsListener implements EventListener {

    @Override
    public void handleEvent(Event event) throws ClientException {
        String eventName = event.getName();
        if (!DocumentEventTypes.BEFORE_DOC_UPDATE.equals(eventName)
                || !(event.getContext() instanceof DocumentEventContext)) {
            return;
        }

        DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
        DocumentModel doc = docCtx.getSourceDocument();
        if (doc.hasFacet(Constants.TENANT_CONFIG_FACET)) {
            Property property = doc.getProperty(Constants.TENANT_ADMINISTRATORS_PROPERTY);
            if (property.isDirty()) {
                // flush the principals cache
                EventService eventService = Framework.getLocalService(EventService.class);
                eventService.sendEvent(new org.nuxeo.runtime.services.event.Event(
                        UserManagerImpl.USERMANAGER_TOPIC,
                        "invalidateAllPrincipals", null, null));
            }
        }
    }

}
