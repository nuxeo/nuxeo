/*
 * (C) Copyright 2011-2018 Nuxeo (http://nuxeo.com/) and others.
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

@XObject("configuration")
@XRegistry(compatWarnOnMerge = true)
public class UserRegistrationConfiguration {

    private static Log log = LogFactory.getLog(UserRegistrationConfiguration.class);

    public static final String DEFAULT_CONFIGURATION_NAME = "default_registration";

    @XNode(value = "@name", defaultAssignment = DEFAULT_CONFIGURATION_NAME)
    @XRegistryId
    private String name;

    @XNode("requestDocType")
    private String requestDocType;

    @XNode("userInfo/schemaName")
    private String userInfoSchemaName = "userinfo";

    @XNode("userInfo/usernameField")
    private String userInfoUsernameField = "userinfo:login";

    @XNode("userInfo/emailField")
    private String userInfoEmailField = "userinfo:email";

    @XNode("userInfo/firstnameField")
    private String userInfoFirstnameField = "userinfo:firstName";

    @XNode("userInfo/lastnameField")
    private String userInfoLastnameField = "userinfo:lastName";

    @XNode("userInfo/companyField")
    private String userInfoCompanyField = "userinfo:company";

    /**
     * @deprecated since 7.3. Not used anymore, the password is not stored.
     */
    @Deprecated(since = "7.3")
    @XNode("userInfo/passwordField")
    private String userInfoPasswordField = "userinfo:password";

    @XNode("userInfo/groupsField")
    private String userInfoGroupsField = "userinfo:groups";

    @XNode("userInfo/tenantIdField")
    private String userInfoTenantIdField = "userinfo:tenantId";

    @XNode("container/docType")
    private String containerDocType;

    @XNode("container/parentPath")
    private String containerParentPath;

    @XNode("container/name")
    private String containerName;

    @XNode("container/title")
    private String containerTitle;

    @XNode("validationEmail/title")
    private String validationEmailTitle;

    @XNode("validationEmail/template")
    private String validationEmailTemplate;

    @XNode("successEmail/title")
    private String successEmailTitle;

    @XNode("successEmail/template")
    private String successEmailTemplate;

    @XNode("reviveEmail/title")
    private String reviveEmailTitle;

    @XNode("reviveEmail/template")
    private String reviveEmailTemplate;

    @XNode("registrationUserFactory")
    private Class<? extends InvitationUserFactory> registrationUserFactory;

    @XNode("validationRelUrl")
    private String validationRelUrl;

    @XNode("enterPasswordUrl")
    private String enterPasswordUrl;

    @XNode("invitationLayout")
    private String invitationLayout = "user_invitation_info";

    @XNode("listingContentView")
    private String listingLocalContentView = "local_user_requests_view";

    public String getRequestDocType() {
        return requestDocType;
    }

    public String getContainerDocType() {
        return containerDocType;
    }

    public String getContainerParentPath() {
        return containerParentPath;
    }

    public String getContainerName() {
        return containerName;
    }

    public String getContainerTitle() {
        return containerTitle;
    }

    public String getValidationEmailTitle() {
        return validationEmailTitle;
    }

    public String getValidationEmailTemplate() {
        return validationEmailTemplate;
    }

    public String getSuccessEmailTitle() {
        return successEmailTitle;
    }

    public String getSuccessEmailTemplate() {
        return successEmailTemplate;
    }

    public Class<? extends InvitationUserFactory> getRegistrationUserFactory() {
        return registrationUserFactory;
    }

    public String getValidationRelUrl() {
        if (StringUtils.isBlank(validationRelUrl)) {
            log.info("Configuration " + name + " has empty validation url");
            return "";
        }
        return validationRelUrl;
    }

    public String getEnterPasswordUrl() {
        if (StringUtils.isBlank(enterPasswordUrl)) {
            log.info("Configuration " + name + " has empty validation url");
            return "";
        }
        if (enterPasswordUrl.startsWith("/")) {
            enterPasswordUrl = enterPasswordUrl.substring(1);
        }
        return enterPasswordUrl;
    }

    public String getReviveEmailTitle() {
        return reviveEmailTitle;
    }

    public String getReviveEmailTemplate() {
        return reviveEmailTemplate;
    }

    public String getUserInfoSchemaName() {
        return userInfoSchemaName;
    }

    public String getUserInfoUsernameField() {
        return userInfoUsernameField;
    }

    public String getUserInfoEmailField() {
        return userInfoEmailField;
    }

    public String getUserInfoFirstnameField() {
        return userInfoFirstnameField;
    }

    public String getUserInfoLastnameField() {
        return userInfoLastnameField;
    }

    public String getUserInfoCompanyField() {
        return userInfoCompanyField;
    }

    /**
     * @deprecated since 7.3. Not used anymore, the password is not stored.
     */
    @Deprecated(since = "7.3")
    public String getUserInfoPasswordField() {
        return userInfoPasswordField;
    }

    public String getUserInfoGroupsField() {
        return userInfoGroupsField;
    }

    public String getUserInfoTenantIdField() {
        return userInfoTenantIdField;
    }

    public String getName() {
        return name;
    }

    public String getInvitationLayout() {
        return invitationLayout;
    }

    public String getListingLocalContentView() {
        return listingLocalContentView;
    }

}
