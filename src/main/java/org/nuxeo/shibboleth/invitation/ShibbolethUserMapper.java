/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */

package org.nuxeo.shibboleth.invitation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.user.invite.UserInvitationService;
import org.nuxeo.ecm.user.registration.UserRegistrationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.usermapper.extension.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User mapper for handling user post creation when authenticating with Shibboleth (by invitation)
 *
 * @since 7.4
 */
public class ShibbolethUserMapper implements UserMapper {

    private static final Logger log = LoggerFactory.getLogger(ShibbolethUserMapper.class);


    public static final String DEFAULT_REGISTRATION = "default_registration";

    protected static String userSchemaName = "user";

    protected static String groupSchemaName = "group";

    protected UserManager userManager;

    @Override
    public NuxeoPrincipal getOrCreateAndUpdateNuxeoPrincipal(Object userObject) {
        return getOrCreateAndUpdateNuxeoPrincipal(userObject, true, true, null);
    }

    protected UserInvitationService fetchService() {
        return Framework.getLocalService(UserRegistrationService.class);
    }

    @Override
    public NuxeoPrincipal getOrCreateAndUpdateNuxeoPrincipal(Object userObject, boolean createIfNeeded, boolean update,
            Map<String, Serializable> params) {

        ShibbolethUserInfo userInfo = new ShibbolethUserInfo((String) ((Map) userObject).get("username"),
                (String) ((Map) userObject).get("password"), (String) ((Map) userObject).get("firstName"),
                (String) ((Map) userObject).get("lastName"), (String) ((Map) userObject).get("company"));

        // Check if email has been provided and if invitation has been assigned to a user with email as username
        DocumentModel userDoc = null;
        String mail = (String) ((Map) userObject).get("email");
        String userName = userInfo.getUserName();
        if (mail != null && !mail.isEmpty()) {
            userDoc = findUser(mail);
        }
        if (userDoc != null) {
            // Update target document ACLs;
            DocumentModelList targetDocs = getTargetDocuments(mail);
            for (DocumentModel targetDoc : targetDocs) {
                for (ACL acl : targetDoc.getACP().getACLs()) {
                    for (ACE oldACE : acl.getACEs()) {
                        if (oldACE.getUsername().equals(mail)) {
                            ACE newACE = ACE.builder(userName, oldACE.getPermission())
                                            .creator(oldACE.getCreator())
                                            .begin(oldACE.getBegin())
                                            .end(oldACE.getEnd())
                                            .build();
                            new UnrestrictedSessionRunner(getTargetRepositoryName()) {
                                @Override
                                public void run() {
                                    session.replaceACE(targetDoc.getRef(), acl.getName(), oldACE, newACE);
                                }
                            }.runUnrestricted();
                        }
                    }
                }
            }
        } else {
            userDoc = findUser(userInfo.getUserName());
        }

        if (userDoc == null) {
            userDoc = createUser(userInfo);
        }
        // Update user with related infos
        userDoc = updateUser(userDoc, userInfo);

        String userId = (String) userDoc.getPropertyValue(userManager.getUserIdField());
        return userManager.getPrincipal(userId);
    }

    protected DocumentModel createUser(ShibbolethUserInfo userInfo) {
        DocumentModel userDoc;
        try {
            userDoc = userManager.getBareUserModel();
            userDoc.setPropertyValue(userManager.getUserIdField(), userInfo.getUserName());
            userDoc.setPropertyValue(userManager.getUserEmailField(), userInfo.getUserName());
            userManager.createUser(userDoc);
        } catch (NuxeoException e) {
            String message = "Error while creating user [" + userInfo.getUserName() + "] in UserManager";
            log.error(message, e);
            throw new RuntimeException(message);
        }
        return userDoc;
    }

    protected DocumentModelList getTargetDocuments(String userId) {
        UserInvitationService userInvitationService = Framework.getLocalService(UserRegistrationService.class);
        final DocumentModelList registrationDocs = new DocumentModelListImpl();
        new UnrestrictedSessionRunner(getTargetRepositoryName()) {
            @Override
            public void run() {
                String query = "SELECT * FROM Document WHERE ecm:currentLifeCycleState != 'validated' AND "
                        + "ecm:mixinType = '"
                        + userInvitationService.getConfiguration(DEFAULT_REGISTRATION).getRequestDocType() + "' AND "
                        + userInvitationService.getConfiguration(DEFAULT_REGISTRATION).getUserInfoUsernameField()
                        + " = '%s' AND ecm:isCheckedInVersion = 0";
                query = String.format(query, userId);
                registrationDocs.addAll(session.query(query));
            }
        }.runUnrestricted();
        return registrationDocs;
    }

    @Override
    public void init(Map<String, String> params) throws Exception {
        userManager = Framework.getLocalService(UserManager.class);
        userSchemaName = userManager.getUserSchemaName();
        groupSchemaName = userManager.getGroupSchemaName();
    }

    private DocumentModel findUser(String userName) {
        Map<String, Serializable> query = new HashMap<>();
        query.put(userManager.getUserIdField(), userName);
        DocumentModelList users = userManager.searchUsers(query, null);

        if (users.isEmpty()) {
            return null;
        }
        return users.get(0);
    }

    private DocumentModel updateUser(DocumentModel userDoc, ShibbolethUserInfo userInfo) {
        userDoc.setPropertyValue(userManager.getUserIdField(), userInfo.getUserName());
        userDoc.setPropertyValue(userManager.getUserEmailField(), userInfo.getUserName());
        userDoc.setProperty(userSchemaName, "firstName", userInfo.getFirstName());
        userDoc.setProperty(userSchemaName, "lastName", userInfo.getLastName());
        userDoc.setProperty(userSchemaName, "password", userInfo.getPassword());
        userDoc.setProperty(userSchemaName, "company", userInfo.getCompany());
        userManager.updateUser(userDoc);
        return userDoc;
    }

    @Override
    public Object wrapNuxeoPrincipal(NuxeoPrincipal principal, Object nativePrincipal, Map<String, Serializable> params) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void release() {
    }

    public String getTargetRepositoryName() {
        return Framework.getService(RepositoryManager.class).getDefaultRepositoryName();
    }
}
