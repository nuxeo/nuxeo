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
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.user.invite.DefaultInvitationUserFactory;
import org.nuxeo.ecm.user.invite.UserRegistrationConfiguration;
import org.nuxeo.ecm.user.invite.UserRegistrationException;

public class DefaultRegistrationUserFactory extends
        DefaultInvitationUserFactory implements RegistrationUserFactory {

    private static final Log log = LogFactory.getLog(DefaultRegistrationUserFactory.class);

    @Override
    public DocumentModel doAddDocumentPermission(CoreSession session,
            DocumentModel registrationDoc,
            UserRegistrationConfiguration configuration) throws ClientException {
        String docId = (String) registrationDoc.getPropertyValue(DocumentRegistrationInfo.DOCUMENT_ID_FIELD);

        if (StringUtils.isEmpty(docId)) {
            log.info("No document rights needed");
            return null;
        }
        String login = (String) registrationDoc.getPropertyValue(configuration.getUserInfoUsernameField());
        String permission = (String) registrationDoc.getPropertyValue(DocumentRegistrationInfo.DOCUMENT_RIGHT_FIELD);
        if (StringUtils.isEmpty(permission)) {
            throw new UserRegistrationException("Permission must be specified");
        }

        DocumentModel document = session.getDocument(new IdRef(docId));
        if (!document.getACP().getAccess(login, permission).toBoolean()) {
            ACE ace = new ACE(login, permission, true);
            // Always append ACL to the first place to be after the block
            // rights inheritance ACE.
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
