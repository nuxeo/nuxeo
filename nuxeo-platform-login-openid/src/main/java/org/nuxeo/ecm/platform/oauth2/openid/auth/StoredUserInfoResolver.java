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


public class StoredUserInfoResolver extends UserResolver {

    private OpenIDUserInfoStore userInfoStore;

    private static final Log log = LogFactory.getLog(UserResolverHelper.class);

    StoredUserInfoResolver(OpenIDConnectProvider provider) {
        super(provider);
    }

    public OpenIDUserInfoStore getUserInfoStore() {
        if (userInfoStore == null) {
            userInfoStore = new OpenIDUserInfoStoreImpl(getProvider().getName());
        }
        return userInfoStore;
    }

    @Override
    public String findNuxeoUser(OpenIDUserInfo userInfo) {

        try {

            getUserInfoStore().getNuxeoLogin(userInfo);


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

    public String findOrCreateNuxeoUser(String providerName, OpenIDUserInfo userInfo) {
        String user = findNuxeoUser(userInfo);
        if (user == null) {
            throw new UnsupportedOperationException(
                    "User creation is not implemented for now");
        }
        return user;
    }
}

