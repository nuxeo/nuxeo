/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.usermapper.extension;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.user.center.profile.UserProfileService;
import org.nuxeo.runtime.api.Framework;

/**
 * Provide default implementation for interaction with the {@link UserManager}.
 *
 * @author tiry
 * @since 7.4
 */
public abstract class AbstractUserMapper implements UserMapper {

    protected static final Log log = LogFactory.getLog(AbstractUserMapper.class);

    public AbstractUserMapper() {
    }

    @Override
    public NuxeoPrincipal getOrCreateAndUpdateNuxeoPrincipal(Object userObject) {
        return getOrCreateAndUpdateNuxeoPrincipal(userObject, true, true, null);
    }

    @Override
    public NuxeoPrincipal getOrCreateAndUpdateNuxeoPrincipal(Object userObject, boolean createIfNeeded, boolean update,
            Map<String, Serializable> params) {

        DocumentModel userModel = null;

        Map<String, Serializable> searchAttributes = new HashMap<>();
        Map<String, Serializable> userAttributes = new HashMap<>();
        final Map<String, Serializable> profileAttributes = new HashMap<>();

        if (params != null) {
            searchAttributes.putAll(params);
        }

        resolveAttributes(userObject, searchAttributes, userAttributes, profileAttributes);

        UserManager userManager = getUserManager();

        String userId = (String) searchAttributes.get(userManager.getUserIdField());

        if (userId != null) {
            userModel = userManager.getUserModel(userId);
        }
        if (userModel == null) {
            if (searchAttributes.size() > 0) {
                DocumentModelList userDocs = userManager.searchUsers(searchAttributes, Collections.<String> emptySet());
                if (userDocs.size() > 1) {
                    log.warn("Can not map user with filter " + searchAttributes.toString() + " : too many results");
                }
                if (userDocs.size() == 1) {
                    userModel = userDocs.get(0);
                }
            }
        }
        if (userModel != null) {
            if (update) {
                updatePrincipal(userAttributes, userModel);
            }
        } else {
            if (!createIfNeeded) {
                return null;
            }
            for (String k : searchAttributes.keySet()) {
                if (!userAttributes.containsKey(k)) {
                    userAttributes.put(k, searchAttributes.get(k));
                }
            }
            userModel = createPrincipal(userAttributes);
        }

        if (userModel != null && profileAttributes.size() > 0 && update) {
            UserProfileService UPS = Framework.getService(UserProfileService.class);
            if (UPS != null) {

                final String login = (String) userModel.getPropertyValue(userManager.getUserIdField());

                String repoName = Framework.getService(RepositoryManager.class).getDefaultRepositoryName();
                new UnrestrictedSessionRunner(repoName) {
                    @Override
                    public void run() {
                        DocumentModel profile = UPS.getUserProfileDocument(login, session);
                        updateProfile(session, profileAttributes, profile);
                    }
                }.runUnrestricted();
            }
        }

        if (userModel != null) {
            userId = (String) userModel.getPropertyValue(userManager.getUserIdField());
            return userManager.getPrincipal(userId);
        }
        return null;
    }

    protected void updatePrincipal(Map<String, Serializable> attributes, DocumentModel userModel) {
        UserManager userManager = getUserManager();
        DataModel dm = userModel.getDataModel(userManager.getUserSchemaName());
        for (String key : attributes.keySet()) {
            dm.setValue(key, attributes.get(key));
        }
        userManager.updateUser(userModel);
    }

    protected void updateProfile(CoreSession session, Map<String, Serializable> attributes, DocumentModel userProfile) {
        for (String key : attributes.keySet()) {
            userProfile.setPropertyValue(key, attributes.get(key));
        }
        session.saveDocument(userProfile);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected DocumentModel createPrincipal(Map<String, Serializable> attributes) {
        UserManager userManager = getUserManager();
        DocumentModel userModel = userManager.getBareUserModel();
        userModel.getDataModel(userManager.getUserSchemaName()).setMap((Map) attributes);
        DocumentModel userDoc;
        try {
            userDoc = Framework.doPrivileged(() -> userManager.createUser(userModel));
        } catch (NuxeoException e) {
            log.error("Error while creating user in UserManager", e);
            return null;
        }
        return userDoc;
    }

    protected abstract void resolveAttributes(Object userObject, Map<String, Serializable> searchAttributes,
            Map<String, Serializable> userAttributes, Map<String, Serializable> profileAttributes);

    public UserManager getUserManager() {
        return Framework.getService(UserManager.class);
    }
}
