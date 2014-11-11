package org.nuxeo.ecm.platform.ui.web.auth.jboss;

import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfoCallbackHandler;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoCallbackHandlerFactory;

public class JBossUserIdentificationCallbackHandlerFactory implements
        NuxeoCallbackHandlerFactory {

    public UserIdentificationInfoCallbackHandler createCallbackHandler(
            UserIdentificationInfo userIdent) {
        return new JBossUserIdentificationInfoCallbackHandler(userIdent);
    }

}
