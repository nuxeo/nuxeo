/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *
 * Contributors:
 *     Nelson Silva <nelson.silva@inevo.pt> - initial API and implementation
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.oauth2.openid.auth;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.oauth2.openid.OpenIDConnectProvider;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper class to manage mapping between identification info comming from the
 * OpenID provider and Nuxeo UserManager.
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.7
 */
public class EmailBasedUserResolver extends UserResolver {

    private static final Log log = LogFactory.getLog(EmailBasedUserResolver.class);

    public EmailBasedUserResolver(OpenIDConnectProvider provider) {
        super(provider);
    }

    @Override
    public String findNuxeoUser(OpenIDUserInfo userInfo) {

        try {
            UserManager userManager = Framework.getLocalService(UserManager.class);
            Map<String, Serializable> query = new HashMap<String, Serializable>();
            query.put(userManager.getUserEmailField(), userInfo.getEmail());

            DocumentModelList users = userManager.searchUsers(query, null);

            if (users.isEmpty()) {
                return null;
            }

            DocumentModel user = users.get(0);
            return (String) user.getPropertyValue(userManager.getUserIdField());

        } catch (ClientException e) {
            log.error("Error while search user in UserManager using email "
                    + userInfo.getEmail(), e);
            return null;
        }
    }

    @Override
    public DocumentModel updateUserInfo(DocumentModel user, OpenIDUserInfo userInfo) {
        try {
            UserManager userManager = Framework.getLocalService(UserManager.class);
            user.setPropertyValue(userManager.getUserEmailField(), userInfo.getEmail());
            userManager.updateUser(user);
        } catch (ClientException e) {
            log.error("Error while search user in UserManager using email "
                    + userInfo.getEmail(), e);
            return null;
        }
        return user;
    }

}
