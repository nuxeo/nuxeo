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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.oauth2.openid.OpenIDConnectProvider;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

public class StoredUserInfoResolver extends UserResolver {

    private OpenIDUserInfoStore userInfoStore;

    private static final Log log = LogFactory.getLog(StoredUserInfoResolver.class);

    public StoredUserInfoResolver(OpenIDConnectProvider provider) {
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
        String nuxeoLogin = getUserInfoStore().getNuxeoLogin(userInfo);
        // Check if the user exists
        try {
            UserManager userManager = Framework.getLocalService(UserManager.class);
            if (userManager.getUserModel(nuxeoLogin) == null) {
                nuxeoLogin = null;
            }

        } catch (ClientException e) {
            log.error("Error while search user in UserManager using email "
                    + userInfo.getEmail(), e);
            return null;
        }
        return nuxeoLogin;
    }

    @Override
    public DocumentModel updateUserInfo(DocumentModel user, OpenIDUserInfo userInfo) {
        try {
            UserManager userManager = Framework.getLocalService(UserManager.class);
            String userId = (String) user.getPropertyValue(userManager.getUserIdField());
            getUserInfoStore().storeUserInfo(userId, userInfo);
        } catch (ClientException e) {
            log.error("Error while updating user info for user "
                    + userInfo.getEmail(), e);
            return null;
        }
        return user;

    }

}

