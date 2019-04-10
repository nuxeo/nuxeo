/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */

package org.nuxeo.shibboleth.invitation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.MoreObjects;
import com.google.common.collect.BiMap;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.platform.shibboleth.service.ShibbolethAuthenticationService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.user.invite.UserInvitationService;
import org.nuxeo.ecm.user.registration.UserRegistrationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;
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
        return Framework.getService(UserRegistrationService.class);
    }

    @Override
    public NuxeoPrincipal getOrCreateAndUpdateNuxeoPrincipal(Object userObject, boolean createIfNeeded, boolean update,
            Map<String, Serializable> params) {

        // Fetching keys from the shibboleth configuration in nuxeo
        ShibbolethAuthenticationService shiboService = Framework.getService(ShibbolethAuthenticationService.class);
        BiMap<String, String> metadata = shiboService.getUserMetadata();
        String usernameKey = MoreObjects.firstNonNull(metadata.get("username"), "username");
        String lastNameKey = MoreObjects.firstNonNull(metadata.get("lastName"), "lastName");
        String firstNameKey = MoreObjects.firstNonNull(metadata.get("firstName"), "firstName");
        String emailKey = MoreObjects.firstNonNull(metadata.get("email"), "email");
        String companyKey = MoreObjects.firstNonNull(metadata.get("company"), "company");
        String passwordKey = MoreObjects.firstNonNull(metadata.get("password"), "password");

        String email = (String) ((Map) userObject).get(emailKey);
        ShibbolethUserInfo userInfo = new ShibbolethUserInfo((String) ((Map) userObject).get(usernameKey),
                (String) ((Map) userObject).get(passwordKey), (String) ((Map) userObject).get(firstNameKey),
                (String) ((Map) userObject).get(lastNameKey), (String) ((Map) userObject).get(companyKey), email);

        // Check if email has been provided and if invitation has been assigned to a user with email as username
        DocumentModel userDoc = null;
        String userName = userInfo.getUserName();
        if (email != null && !email.isEmpty()) {
            userDoc = findUser(userManager.getUserIdField(), email);
        }
        if (userDoc != null && userName != null) {
            DocumentModel finalUserDoc = userDoc; // Effectively final
            TransactionHelper.runInTransaction(() -> updateACP(userName, email, finalUserDoc));
        } else {
            userDoc = userManager.getUserModel(userName);
        }
        if (userDoc == null) {
            userDoc = createUser(userInfo);
        } else {
            updateUser(userDoc, userInfo);
        }

        String userId = (String) userDoc.getPropertyValue(userManager.getUserIdField());
        return userManager.getPrincipal(userId);
    }

    protected void updateACP(String userName, String email, DocumentModel userDoc) {
        new UnrestrictedSessionRunner(getTargetRepositoryName()) {
            @Override
            public void run() {

                NuxeoPrincipal principal = userManager.getPrincipal(
                        (String) userDoc.getProperty(userSchemaName, "username"));
                ArrayList<String> groups = new ArrayList<>(principal.getGroups());

                userManager.deleteUser(userDoc);
                userDoc.setPropertyValue("user:username", userName);
                userDoc.setPropertyValue("user:groups", groups);
                userManager.createUser(userDoc);
                // Fetching the registrations
                UserInvitationService userInvitationService = Framework.getService(UserRegistrationService.class);
                DocumentModelList registrationDocuments = new DocumentModelListImpl();
                String query = "SELECT * FROM Document WHERE ecm:currentLifeCycleState != 'validated' AND "
                        + "ecm:mixinType = '"
                        + userInvitationService.getConfiguration(DEFAULT_REGISTRATION).getRequestDocType() + "' AND "
                        + userInvitationService.getConfiguration(DEFAULT_REGISTRATION).getUserInfoUsernameField()
                        + " = '%s' AND ecm:isVersion = 0";
                query = String.format(query, email);
                registrationDocuments.addAll(session.query(query));
                Map<String, DocumentModel> targetDocuments = new HashMap<>();
                // Fetching the target documents
                for (DocumentModel doc : registrationDocuments) {
                    String docId = (String) doc.getPropertyValue("docinfo:documentId");
                    if (docId != null && !targetDocuments.keySet().contains(docId))
                        targetDocuments.put(docId, session.getDocument(new IdRef(docId)));
                }
                // Update target document ACLs;
                List<DocumentModel> targetDocs = new ArrayList<>(targetDocuments.values());
                for (DocumentModel targetDoc : targetDocs) {
                    for (ACL acl : targetDoc.getACP().getACLs()) {
                        for (ACE oldACE : acl.getACEs()) {
                            if (oldACE.getUsername().equals(email)) {
                                ACE newACE = ACE.builder(userName, oldACE.getPermission())
                                                .creator(oldACE.getCreator())
                                                .begin(oldACE.getBegin())
                                                .end(oldACE.getEnd())
                                                .build();
                                session.replaceACE(targetDoc.getRef(), acl.getName(), oldACE, newACE);
                            }
                        }
                    }
                }
            }
        }.runUnrestricted();
    }

    protected DocumentModel createUser(ShibbolethUserInfo userInfo) {
        DocumentModel userDoc;
        try {
            userDoc = userManager.getBareUserModel();
            userDoc.setPropertyValue(userManager.getUserIdField(), userInfo.getUserName());
            userDoc.setPropertyValue(userManager.getUserEmailField(), userInfo.getUserName());
            Framework.doPrivileged(() -> userManager.createUser(userDoc));
        } catch (NuxeoException e) {
            String message = "Error while creating user [" + userInfo.getUserName() + "] in UserManager";
            log.error(message, e);
            throw new RuntimeException(message);
        }
        return userDoc;
    }

    @Override
    public void init(Map<String, String> params) throws Exception {
        userManager = Framework.getService(UserManager.class);
        userSchemaName = userManager.getUserSchemaName();
        groupSchemaName = userManager.getGroupSchemaName();
    }

    private DocumentModel findUser(String field, String userName) {
        Map<String, Serializable> query = new HashMap<>();
        query.put(field, userName);
        DocumentModelList users = Framework.doPrivileged(() -> userManager.searchUsers(query, null));

        if (users.isEmpty()) {
            return null;
        }
        return users.get(0);
    }

    private void updateUser(DocumentModel userDoc, ShibbolethUserInfo userInfo) {
        userDoc.setPropertyValue(userManager.getUserEmailField(), userInfo.getEmail());
        userDoc.setProperty(userSchemaName, "firstName", userInfo.getFirstName());
        userDoc.setProperty(userSchemaName, "lastName", userInfo.getLastName());
        userDoc.setProperty(userSchemaName, "password", userInfo.getPassword());
        userDoc.setProperty(userSchemaName, "company", userInfo.getCompany());
        Framework.doPrivileged(() -> userManager.updateUser(userDoc));
    }

    @Override
    public Object wrapNuxeoPrincipal(NuxeoPrincipal principal, Object nativePrincipal,
            Map<String, Serializable> params) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void release() {
    }

    public String getTargetRepositoryName() {
        return Framework.getService(RepositoryManager.class).getDefaultRepositoryName();
    }
}
