/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.user.registration;

import static org.nuxeo.ecm.user.registration.DocumentRegistrationInfo.ACL_NAME;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserConfig;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

public class DefaultRegistrationUserFactory implements RegistrationUserFactory {

    private static final Log log = LogFactory.getLog(DefaultRegistrationUserFactory.class);

    @Override
    public NuxeoPrincipal createUser(CoreSession session,
            DocumentModel registrationDoc) throws ClientException,
            UserRegistrationException {
        NuxeoPrincipal user = doCreateUser(session, registrationDoc);
        doPostUserCreation(session, registrationDoc, user);
        return user;
    }

    @Override
    public void doPostUserCreation(CoreSession session,
            DocumentModel registrationDoc, NuxeoPrincipal user)
            throws ClientException, UserRegistrationException {
        // Nothing to do in the default implementation
    }

    @Override
    public NuxeoPrincipal doCreateUser(CoreSession session,
            DocumentModel registrationDoc) throws ClientException,
            UserRegistrationException {
        UserManager userManager = Framework.getLocalService(UserManager.class);

        String email = (String) registrationDoc.getPropertyValue(UserRegistrationInfo.EMAIL_FIELD);
        if (email == null) {
            throw new UserRegistrationException(
                    "Email address must be specififed");
        }

        String login = (String) registrationDoc.getPropertyValue(UserRegistrationInfo.USERNAME_FIELD);
        NuxeoPrincipal user = userManager.getPrincipal(login);
        if (user == null) {
            DocumentModel newUserDoc = userManager.getBareUserModel();
            newUserDoc.setPropertyValue(
                    UserConfig.USERNAME_COLUMN,
                    registrationDoc.getPropertyValue(UserRegistrationInfo.USERNAME_FIELD));
            newUserDoc.setPropertyValue(
                    UserConfig.PASSWORD_COLUMN,
                    registrationDoc.getPropertyValue(UserRegistrationInfo.PASSWORD_FIELD));
            newUserDoc.setPropertyValue(
                    UserConfig.FIRSTNAME_COLUMN,
                    registrationDoc.getPropertyValue(UserRegistrationInfo.FIRSTNAME_FIELD));
            newUserDoc.setPropertyValue(
                    UserConfig.LASTNAME_COLUMN,
                    registrationDoc.getPropertyValue(UserRegistrationInfo.LASTNAME_FIELD));
            newUserDoc.setPropertyValue(
                    UserConfig.EMAIL_COLUMN,
                    registrationDoc.getPropertyValue(UserRegistrationInfo.EMAIL_FIELD));
            newUserDoc.setPropertyValue(
                    UserConfig.COMPANY_COLUMN,
                    registrationDoc.getPropertyValue(UserRegistrationInfo.COMPANY_FIELD));
            userManager.createUser(newUserDoc);
            user = userManager.getPrincipal(login);

            log.info("New user created:" + user.getName());
        } else {
            if (!email.equals(((NuxeoPrincipalImpl) user).getEmail())) {
                throw new UserRegistrationException(
                        "This login is not available");
            }
        }
        return user;
    }

    @Override
    public DocumentModel doAddDocumentPermission(CoreSession session,
            DocumentModel registrationDoc) throws ClientException {
        String docId = (String) registrationDoc.getPropertyValue(DocumentRegistrationInfo.DOCUMENT_ID_FIELD);
        if (StringUtils.isEmpty(docId)) {
            log.info("No document rights needed");
            return null;
        }
        String login = (String) registrationDoc.getPropertyValue(UserRegistrationInfo.USERNAME_FIELD);
        String permission = (String) registrationDoc.getPropertyValue(DocumentRegistrationInfo.DOCUMENT_RIGHT_FIELD);
        if (StringUtils.isEmpty(permission)) {
            throw new UserRegistrationException("Permission must be specified");
        }

        DocumentModel document = session.getDocument(new IdRef(docId));
        if (!document.getACP().getAccess(login, permission).toBoolean()) {
            ACE ace = new ACE(login, permission, true);
            // Always append ACL to the first place to be after the block rights inheritance ACE.
            document.getACP().getOrCreateACL(ACL_NAME).add(0, ace);

            session.setACP(document.getRef(), document.getACP(), true);
        } else {
            log.info(String.format("User %s already have %s on doc %s", login,
                    permission, docId));
        }

        return document;
    }

    @Override
    public void doPostAddDocumentPermission(CoreSession session,
            DocumentModel registrationDoc, DocumentModel document)
            throws ClientException {
        // Nothing to do in the default implementation
    }
}
