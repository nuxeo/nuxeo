package org.nuxeo.ecm.user.registration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

public class DefaultRegistrationUserFactory implements RegistrationUserFactory {

    private static final Log log = LogFactory.getLog(DefaultRegistrationUserFactory.class);

    protected UserManager userManager;

    @Override
    public NuxeoPrincipal createUser(CoreSession session,
            DocumentModel registrationDoc) throws ClientException,
            UserRegistrationException {
        userManager = Framework.getLocalService(UserManager.class);
        String userSchemaName = userManager.getUserSchemaName();

        String email = (String) registrationDoc.getProperty(userSchemaName,
                userManager.getUserEmailField());
        if (email == null) {
            throw new UserRegistrationException(
                    "Email address must be specififed");
        }

        String login = (String) registrationDoc.getProperty(userSchemaName,
                userManager.getUserIdField());
        NuxeoPrincipal user = userManager.getPrincipal(login);
        if (user == null) {
            DocumentModel newUserDoc = userManager.getBareUserModel();
            DataModel userSchema = newUserDoc.getDataModel(userSchemaName);
            for (String fieldName : userSchema.getMap().keySet()) {
                Object property = registrationDoc.getProperty(userSchemaName,
                        fieldName);
                if (property != null) {
                    userSchema.setValue(fieldName, property);
                }
            }
            newUserDoc = userManager.createUser(newUserDoc);
            user = userManager.getPrincipal(login);
        } else {
            if (email.equals(((NuxeoPrincipalImpl) user).getEmail())) {
                throw new UserRegistrationException(
                        "You already have an account");
            } else {
                throw new UserRegistrationException(
                        "This login is not available");
            }
        }
        log.info("New user created:" + user.getName());

        doPostUserCreation(session, registrationDoc, user);

        return user;
    }

    @Override
    public void doPostUserCreation(CoreSession session,
            DocumentModel registrationDoc, NuxeoPrincipal user)
            throws ClientException, UserRegistrationException {

    }

}
