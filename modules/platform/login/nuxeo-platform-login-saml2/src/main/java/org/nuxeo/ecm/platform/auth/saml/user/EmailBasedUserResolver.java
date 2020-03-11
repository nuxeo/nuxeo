/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
package org.nuxeo.ecm.platform.auth.saml.user;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.auth.saml.SAMLCredential;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

public class EmailBasedUserResolver extends AbstractUserResolver {

    private static final Log log = LogFactory.getLog(EmailBasedUserResolver.class);

    @Override
    public String findNuxeoUser(SAMLCredential credential) {

        try {
            UserManager userManager = Framework.getService(UserManager.class);
            Map<String, Serializable> query = new HashMap<>();
            query.put(userManager.getUserEmailField(), credential.getNameID().getValue());

            DocumentModelList users = userManager.searchUsers(query, null);

            if (users.isEmpty()) {
                return null;
            }

            DocumentModel user = users.get(0);
            return (String) user.getPropertyValue(userManager.getUserIdField());

        } catch (NuxeoException e) {
            log.error("Error while search user in UserManager using email " + credential.getNameID().getValue(), e);
            return null;
        }
    }

    @Override
    public DocumentModel updateUserInfo(DocumentModel user, SAMLCredential credential) {
        try {
            UserManager userManager = Framework.getService(UserManager.class);
            user.setPropertyValue(userManager.getUserEmailField(), credential.getNameID().getValue());
        } catch (NuxeoException e) {
            log.error("Error while search user in UserManager using email " + credential.getNameID().getValue(), e);
            return null;
        }
        return user;
    }

    @Override
    public String getLoginName(SAMLCredential userInfo) {
        String email = userInfo.getNameID().getValue();
        return email;
    }

}
