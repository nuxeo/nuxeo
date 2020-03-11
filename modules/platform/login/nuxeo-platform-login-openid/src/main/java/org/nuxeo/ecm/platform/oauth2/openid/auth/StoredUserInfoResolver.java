/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nelson Silva <nelson.silva@inevo.pt> - initial API and implementation
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.oauth2.openid.auth;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
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

        // Check if the user exists
        try {
            UserManager userManager = Framework.getService(UserManager.class);

            return Framework.doPrivileged(() -> {
                String userLogin = getUserInfoStore().getNuxeoLogin(userInfo);
                DocumentModel user = userManager.getUserModel(userLogin);

                return user != null ? userLogin : null;
            });
        } catch (NuxeoException e) {
            log.error("Error while search user in UserManager using email " + userInfo.getEmail(), e);
            return null;
        }
    }

    @Override
    public DocumentModel updateUserInfo(DocumentModel user, OpenIDUserInfo userInfo) {
        try {
            UserManager userManager = Framework.getService(UserManager.class);
            String userId = (String) user.getPropertyValue(userManager.getUserIdField());
            Framework.doPrivileged(() -> getUserInfoStore().storeUserInfo(userId, userInfo));
        } catch (NuxeoException e) {
            log.error("Error while updating user info for user " + userInfo.getEmail(), e);
            return null;
        }
        return user;

    }

}
