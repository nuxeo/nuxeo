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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.auth.saml.SAMLCredential;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class EmailBasedUserResolver extends UserResolver {

    private static final Log log = LogFactory.getLog(
            EmailBasedUserResolver.class);

    @Override
    public String findNuxeoUser(SAMLCredential credential) {

        try {
            UserManager userManager = Framework.getLocalService(
                    UserManager.class);
            Map<String, Serializable> query = new HashMap<>();
            query.put(userManager.getUserEmailField(),
                    credential.getNameID().getValue());

            DocumentModelList users = userManager.searchUsers(query, null);

            if (users.isEmpty()) {
                return null;
            }

            DocumentModel user = users.get(0);
            return (String) user.getPropertyValue(userManager.getUserIdField());

        } catch (ClientException e) {
            log.error("Error while search user in UserManager using email "
                    + credential.getNameID().getValue(), e);
            return null;
        }
    }

    @Override
    public DocumentModel updateUserInfo(DocumentModel user,
            SAMLCredential credential) {
        try {
            UserManager userManager = Framework.getLocalService(
                    UserManager.class);
            user.setPropertyValue(userManager.getUserEmailField(),
                    credential.getNameID().getValue());
        } catch (ClientException e) {
            log.error("Error while search user in UserManager using email "
                    + credential.getNameID().getValue(), e);
            return null;
        }
        return user;
    }

}
