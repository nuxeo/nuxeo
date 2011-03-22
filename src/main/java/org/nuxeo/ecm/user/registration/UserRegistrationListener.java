package org.nuxeo.ecm.user.registration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

public class UserRegistrationListener implements EventListener {

    protected static Log log = LogFactory.getLog(UserRegistrationListener.class);

    public void handleEvent(Event event) throws ClientException {

        if(!event.getName().equals(UserRegistrationService.REGISTRATION_VALIDATED_EVENT)) {
            return;
        }

        EventContext ctx = event.getContext();
        if (ctx instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) ctx;
            DocumentModel registration = docCtx.getSourceDocument();

            try {
                UserRegistrationService userRegistrationService = Framework.getService(UserRegistrationService.class);
                NuxeoPrincipal principal = userRegistrationService.createUser(ctx.getCoreSession(), registration);
                docCtx.setProperty("registeredUser", principal);
            }
            catch (Exception e) {
                event.markRollBack();
                throw new ClientException("Unable to complete registration", e);
            }

        }
    }

}
