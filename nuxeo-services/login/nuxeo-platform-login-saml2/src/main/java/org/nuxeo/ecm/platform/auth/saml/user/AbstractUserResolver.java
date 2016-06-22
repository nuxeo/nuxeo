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

import java.security.Principal;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.auth.saml.SAMLCredential;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

public abstract class AbstractUserResolver implements UserResolver {

    private static final Log log = LogFactory.getLog(AbstractUserResolver.class);


    public abstract String findNuxeoUser(SAMLCredential userInfo);

    public abstract String getLoginName(SAMLCredential userInfo);

    public DocumentModel createNuxeoUser(String nuxeoLogin) {
        DocumentModel userDoc;

        try {
            UserManager userManager = Framework.getService(UserManager.class);

            userDoc = userManager.getBareUserModel();
            userDoc.setPropertyValue(userManager.getUserIdField(), nuxeoLogin);

            userManager.createUser(userDoc);

        } catch (NuxeoException e) {
            log.error("Error while creating user " + nuxeoLogin + "in UserManager", e);
            return null;
        }

        return userDoc;
    }

    public abstract DocumentModel updateUserInfo(DocumentModel user, SAMLCredential userInfo);

    @Override
    public String findOrCreateNuxeoUser(SAMLCredential userInfo) {

        String login = getLoginName(userInfo);
        if (login!=null) {
            UserManager userManager = Framework.getService(UserManager.class);
            Principal principal = userManager.getPrincipal(login);
            if (principal!=null) {
                return login;
            }
        }
        String user = findNuxeoUser(userInfo);
        if (user == null) {
            DocumentModel userDoc = createNuxeoUser(login);
            updateUserInfo(userDoc, userInfo);
        }
        return user;
    }

    @Override
    public void init(Map<String, String> parameters) {
        //NOP
    }

}
