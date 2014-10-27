/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
package org.nuxeo.ecm.platform.auth.saml.user;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.auth.saml.SAMLCredential;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

import java.util.List;

public abstract class UserResolver {

    private static final Log log = LogFactory.getLog(UserResolver.class);

    public abstract String findNuxeoUser(SAMLCredential userInfo);

    public DocumentModel createNuxeoUser(String nuxeoLogin) {
        DocumentModel userDoc;

        try {
            UserManager userManager = Framework.getLocalService(UserManager.class);

            userDoc = userManager.getBareUserModel();
            userDoc.setPropertyValue(userManager.getUserIdField(), nuxeoLogin);

            userManager.createUser(userDoc);

        } catch (ClientException e) {
            log.error("Error while creating user " + nuxeoLogin +
                    "in UserManager", e);
            return null;
        }

        return userDoc;
    }

    public abstract DocumentModel updateUserInfo(DocumentModel user,
            SAMLCredential userInfo);

    public String findOrCreateNuxeoUser(SAMLCredential userInfo) {
        String user = findNuxeoUser(userInfo);
        if (user == null) {
            user = generateRandomUserId();
            DocumentModel userDoc = createNuxeoUser(user);
            updateUserInfo(userDoc, userInfo);
        }
        return user;
    }

    protected String generateRandomUserId() {
        String userId = null;
        try {
            UserManager userManager = Framework.getLocalService(
                    UserManager.class);
            List<String> userIds = userManager.getUserIds();

            while (userId == null || userIds.contains(userId)) {
                userId = "user_" + RandomStringUtils.randomNumeric(4);
            }
        } catch (ClientException e) {
            log.error("Error while generating random user id", e);
            return null;
        }
        return userId;
    }
}