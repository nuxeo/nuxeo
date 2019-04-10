/*
 * (C) Copyright 2011-2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.user.registration;

import static org.nuxeo.ecm.user.registration.DocumentRegistrationInfo.ACL_NAME;

import java.util.Calendar;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.user.invite.DefaultInvitationUserFactory;
import org.nuxeo.ecm.user.invite.UserRegistrationConfiguration;
import org.nuxeo.ecm.user.invite.UserRegistrationException;

public class DefaultRegistrationUserFactory extends DefaultInvitationUserFactory implements RegistrationUserFactory {

    private static final Log log = LogFactory.getLog(DefaultRegistrationUserFactory.class);

    @Override
    public DocumentModel doAddDocumentPermission(CoreSession session, DocumentModel registrationDoc,
            UserRegistrationConfiguration configuration) {
        String docId = (String) registrationDoc.getPropertyValue(DocumentRegistrationInfo.DOCUMENT_ID_FIELD);

        if (StringUtils.isEmpty(docId)) {
            log.info("No document rights needed");
            return null;
        }
        String login = (String) registrationDoc.getPropertyValue(configuration.getUserInfoUsernameField());
        String permission = (String) registrationDoc.getPropertyValue(DocumentRegistrationInfo.DOCUMENT_RIGHT_FIELD);
        Calendar beginCal = (Calendar) registrationDoc.getPropertyValue(DocumentRegistrationInfo.DOCUMENT_BEGIN_FIELD);
        Calendar endCal = (Calendar) registrationDoc.getPropertyValue(DocumentRegistrationInfo.DOCUMENT_END_FIELD);
        if (StringUtils.isEmpty(permission)) {
            throw new UserRegistrationException("Permission must be specified");
        }

        DocumentModel document = session.getDocument(new IdRef(docId));
        if (!document.getACP().getAccess(login, permission).toBoolean()) {
            ACE ace = ACE.builder(login, permission).isGranted(true).begin(beginCal).end(endCal).build();
            // Always append ACL to the first place to be after the block
            // rights inheritance ACE.
            document.getACP().getOrCreateACL(ACL_NAME).add(0, ace);

            session.setACP(document.getRef(), document.getACP(), true);
        } else {
            log.info(String.format("User %s already have %s on doc %s", login, permission, docId));
        }

        return document;
    }

    @Override
    public void doPostAddDocumentPermission(CoreSession session, DocumentModel registrationDoc,
            DocumentModel document) {
        // Nothing to do in the default implementation
    }
}
