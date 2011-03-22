package org.nuxeo.ecm.user.registration;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

public interface RegistrationUserFactory {

    NuxeoPrincipal createUser(CoreSession session, DocumentModel registrationDoc) throws ClientException, UserRegistrationException;

    void doPostUserCreation(CoreSession session, DocumentModel registrationDoc, NuxeoPrincipal user) throws ClientException, UserRegistrationException;

}
