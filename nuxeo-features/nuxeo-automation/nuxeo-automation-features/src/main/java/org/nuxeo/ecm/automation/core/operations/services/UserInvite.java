/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.services;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.user.invite.UserInvitationService;
import org.nuxeo.ecm.user.invite.UserInvitationService.ValidationMethod;
import org.nuxeo.ecm.user.invite.UserRegistrationConfiguration;

/**
 * Simple operation to invite a User.
 *
 * @since 8.2
 */
@Operation(id = UserInvite.ID, category = Constants.CAT_USERS_GROUPS, label = "Invite a user",
        description = "Stores a registration request and returns its ID.")
public class UserInvite {

    public static final String ID = "User.Invite";

    @Context
    protected UserManager userManager;

    @Context
    protected UserInvitationService invitationService;

    @Param(name = "validationMethod", required = false)
    protected ValidationMethod validationMethod = ValidationMethod.EMAIL;

    @Param(name = "autoAccept", required = false)
    protected boolean autoAccept = true;

    @Param(name = "info", required = false)
    protected Map<String, Serializable> info = new HashMap<>();

    @Param(name = "comment", required = false)
    protected String comment;

    @OperationMethod
    public String run(NuxeoPrincipal user) {
        DocumentModel invitation = invitationService.getUserRegistrationModel(null);

        UserRegistrationConfiguration config = invitationService.getConfiguration();
        invitation.setPropertyValue(config.getUserInfoUsernameField(),  user.getName());
        invitation.setPropertyValue(config.getUserInfoFirstnameField(), user.getFirstName());
        invitation.setPropertyValue(config.getUserInfoLastnameField(), user.getLastName());
        invitation.setPropertyValue(config.getUserInfoEmailField(),  user.getEmail());
        invitation.setPropertyValue(config.getUserInfoGroupsField(), user.getGroups().toArray());
        invitation.setPropertyValue(config.getUserInfoTenantIdField(), user.getTenantId());
        invitation.setPropertyValue(config.getUserInfoCompanyField(), user.getCompany());
        invitation.setPropertyValue("registration:comment", comment);

        return invitationService.submitRegistrationRequest(invitation, info, validationMethod, autoAccept);
    }
}
