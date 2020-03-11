/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.user.invite;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserConfig;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

public class DefaultInvitationUserFactory implements InvitationUserFactory {

    private static final Log log = LogFactory.getLog(DefaultInvitationUserFactory.class);

    public static final String PASSWORD_KEY = "invitationPassword";

    @Override
    public void doPostUserCreation(CoreSession session, DocumentModel registrationDoc, NuxeoPrincipal user)
            throws UserRegistrationException {
        // Nothing to do in the default implementation
    }

    @Override
    public NuxeoPrincipal doCreateUser(CoreSession session, DocumentModel registrationDoc,
            UserRegistrationConfiguration configuration) throws UserRegistrationException {
        UserManager userManager = Framework.getService(UserManager.class);

        String email = (String) registrationDoc.getPropertyValue(configuration.getUserInfoEmailField());
        if (email == null) {
            throw new UserRegistrationException("Email address must be specififed");
        }

        String login = (String) registrationDoc.getPropertyValue(configuration.getUserInfoUsernameField());
        NuxeoPrincipal user = userManager.getPrincipal(login);
        if (user == null) {

            if (!isSameTenant(registrationDoc, configuration)) {
                throw new UserRegistrationException("Can only invite in same tenant");
            }

            List<String> groups = filterGroups(registrationDoc, configuration);

            DocumentModel newUserDoc = userManager.getBareUserModel();
            newUserDoc.setPropertyValue(UserConfig.USERNAME_COLUMN, login);
            newUserDoc.setPropertyValue(UserConfig.PASSWORD_COLUMN,
                    registrationDoc.getContextData(PASSWORD_KEY));
            newUserDoc.setPropertyValue(UserConfig.FIRSTNAME_COLUMN,
                    registrationDoc.getPropertyValue(configuration.getUserInfoFirstnameField()));
            newUserDoc.setPropertyValue(UserConfig.LASTNAME_COLUMN,
                    registrationDoc.getPropertyValue(configuration.getUserInfoLastnameField()));
            newUserDoc.setPropertyValue(UserConfig.EMAIL_COLUMN,
                    registrationDoc.getPropertyValue(configuration.getUserInfoEmailField()));
            newUserDoc.setPropertyValue(UserConfig.COMPANY_COLUMN,
                    registrationDoc.getPropertyValue(configuration.getUserInfoCompanyField()));
            newUserDoc.setPropertyValue(UserConfig.GROUPS_COLUMN, groups.toArray());
            newUserDoc.setPropertyValue(UserConfig.TENANT_ID_COLUMN,
                    registrationDoc.getPropertyValue(configuration.getUserInfoTenantIdField()));
            userManager.createUser(newUserDoc);
            user = userManager.getPrincipal(login);

            log.info("New user created:" + user.getName());
        } else {
            if (!email.equals(((NuxeoPrincipalImpl) user).getEmail())) {
                throw new UserRegistrationException("This login is not available");
            }
        }
        return user;
    }

    /**
     * Check that the user that initiated the registration is in the same tenant than the user it creates.
     *
     * @param registrationDoc
     * @param configuration
     * @return
     * @since 10.2
     */
    private boolean isSameTenant(DocumentModel registrationDoc, UserRegistrationConfiguration configuration) {
        NuxeoPrincipal originatingPrincipal = getOriginatingPrincipal(registrationDoc);

        if (originatingPrincipal == null) {
            // Should never occur, but just in case.
            return registrationDoc.getPropertyValue(configuration.getUserInfoTenantIdField()) == null;
        }

        if (originatingPrincipal.isAdministrator()) {
            return true;
        }
        return Objects.equals(registrationDoc.getPropertyValue(configuration.getUserInfoTenantIdField()),
                originatingPrincipal.getTenantId());
    }

    /**
     * Filter group by computing the intersection of the group in the registration doc and the groups of the user that
     * created the request. Administrators accept all groups.
     *
     * @param registrationDoc
     * @param configuration
     * @since 10.2
     */
    @SuppressWarnings("unchecked")
    protected List<String> filterGroups(DocumentModel registrationDoc, UserRegistrationConfiguration configuration) {
        List<String> wantedGroup = (List<String>) registrationDoc.getPropertyValue(
                configuration.getUserInfoGroupsField());

        NuxeoPrincipal originatingPrincipal = getOriginatingPrincipal(registrationDoc);

        if (originatingPrincipal == null) {
            // Should never occur, but just in case.
            return Collections.emptyList();
        }

        return wantedGroup.stream().filter(g -> acceptGroup(originatingPrincipal, g)).collect(Collectors.toList());

    }

    /**
     * Returns the principal that created that registration document
     *
     * @param registrationDoc
     * @return
     * @since 10.2
     */
    private NuxeoPrincipal getOriginatingPrincipal(DocumentModel registrationDoc) {
        String originatingUser = (String) registrationDoc.getPropertyValue(
                UserInvitationComponent.PARAM_ORIGINATING_USER);
        UserManager userManager = Framework.getService(UserManager.class);
        return userManager.getPrincipal(originatingUser);
    }

    /**
     * @param originatingPrincipal
     * @param groupName
     * @return
     * @since 10.2
     */
    protected boolean acceptGroup(NuxeoPrincipal originatingPrincipal, String groupName) {
        return originatingPrincipal.isAdministrator() || originatingPrincipal.getAllGroups().contains(groupName);
    }
}
