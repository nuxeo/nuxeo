package org.nuxeo.ecm.platform.ui.web.auth.interfaces;

import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfoCallbackHandler;

public interface NuxeoCallbackHandlerFactory {

    UserIdentificationInfoCallbackHandler createCallbackHandler(UserIdentificationInfo userIdent);
}
