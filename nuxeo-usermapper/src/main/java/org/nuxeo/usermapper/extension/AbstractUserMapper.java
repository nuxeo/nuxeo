/*
 * (C) Copyright 2006-2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
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
 */
public abstract class AbstractUserMapper implements UserMapper {

    protected static final Log log = LogFactory.getLog(AbstractUserMapper.class);

    protected final UserManager userManager;

    public AbstractUserMapper() {
        userManager = Framework.getService(UserManager.class);
    }

    @Override
    public NuxeoPrincipal getCreateOrUpdateNuxeoPrincipal(Object userObject) {

        DocumentModel userModel = null;

        Map<String, Serializable> searchAttributes = new HashMap<String, Serializable>();
        Map<String, Serializable> userAttributes = new HashMap<String, Serializable>();
        final Map<String, Serializable> profileAttributes = new HashMap<String, Serializable>();

        resolveAttributes(userObject, searchAttributes, userAttributes, profileAttributes);

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
            updatePrincipal(userAttributes, userModel);
        } else {
            for (String k : searchAttributes.keySet() ) {
                if (!userAttributes.containsKey(k)) {
                    userAttributes.put(k, searchAttributes.get(k));
                }
            }
            userModel = createPrincipal(userAttributes);
        }

        if (userModel != null && profileAttributes.size() > 0) {
            UserProfileService UPS = Framework.getService(UserProfileService.class);
            if (UPS != null) {

                final String login = (String) userModel.getPropertyValue(userManager.getUserIdField());

                String repoName = Framework.getService(RepositoryManager.class).getDefaultRepositoryName();
                new UnrestrictedSessionRunner(repoName) {
                    @Override
                    public void run() throws ClientException {
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

        DocumentModel userModel = userManager.getBareUserModel();
        userModel.getDataModel(userManager.getUserSchemaName()).setMap((Map) attributes);
        return userManager.createUser(userModel);
    }

    protected abstract void resolveAttributes(Object userObject, Map<String, Serializable> searchAttributes,
            Map<String, Serializable> userAttributes, Map<String, Serializable> profileAttributes);

}
