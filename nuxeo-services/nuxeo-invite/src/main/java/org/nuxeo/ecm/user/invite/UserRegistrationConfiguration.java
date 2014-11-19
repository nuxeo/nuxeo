/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.user.invite;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("configuration")
public class UserRegistrationConfiguration {

    private static Log log = LogFactory.getLog(UserRegistrationConfiguration.class);

    public static final String DEFAULT_CONFIGURATION_NAME = "default_registration";

    @XNode("@merge")
    private boolean merge = false;

    @XNode("@remove")
    private boolean remove = false;

    @XNode("@name")
    private String name = DEFAULT_CONFIGURATION_NAME;

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
    
    @XNode("userInfo/passwordField")
    private String userInfoPasswordField = "userinfo:password";
    
    @XNode("userInfo/groupsField")
    private String userInfoGroupsField = "userinfo:groups";
    
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

    public String getUserInfoPasswordField() {
        return userInfoPasswordField;
    }

    public String getUserInfoGroupsField() {
        return userInfoGroupsField;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isMerge() {
        return merge;
    }

    public void setMerge(boolean merge) {
        this.merge = merge;
    }

    public boolean isRemove() {
        return remove;
    }

    public void setRemove(boolean remove) {
        this.remove = remove;
    }

    public String getInvitationLayout() {
        return invitationLayout;
    }

    public void setInvitationLayout(String invitationLayout) {
        this.invitationLayout = invitationLayout;
    }

    public String getListingLocalContentView() {
        return listingLocalContentView;
    }

    public void setListingLocalContentView(String listingLocalContentView) {
        this.listingLocalContentView = listingLocalContentView;
    }

    public void mergeWith(UserRegistrationConfiguration other) {
        if (!StringUtils.isEmpty(other.requestDocType)) {
            this.requestDocType = other.requestDocType;
        }

        if (!StringUtils.isEmpty(other.containerDocType)) {
            this.containerDocType = other.containerDocType;
        }

        if (!StringUtils.isEmpty(other.containerParentPath)) {
            this.containerParentPath = other.containerParentPath;
        }

        if (!StringUtils.isEmpty(other.containerName)) {
            this.containerName = other.containerName;
        }

        if (!StringUtils.isEmpty(other.containerTitle)) {
            this.containerTitle = other.containerTitle;
        }

        if (!StringUtils.isEmpty(other.validationEmailTitle)) {
            this.validationEmailTitle = other.validationEmailTitle;
        }

        if (!StringUtils.isEmpty(other.validationEmailTemplate)) {
            this.validationEmailTemplate = other.validationEmailTemplate;
        }

        if (!StringUtils.isEmpty(other.successEmailTitle)) {
            this.successEmailTitle = other.successEmailTitle;
        }

        if (!StringUtils.isEmpty(other.successEmailTemplate)) {
            this.successEmailTemplate = other.successEmailTemplate;
        }

        if (!StringUtils.isEmpty(other.reviveEmailTitle)) {
            this.reviveEmailTitle = other.reviveEmailTitle;
        }

        if (!StringUtils.isEmpty(other.reviveEmailTemplate)) {
            this.reviveEmailTemplate = other.reviveEmailTemplate;
        }

        if (other.getRegistrationUserFactory() != null) {
            this.registrationUserFactory = other.registrationUserFactory;
        }

        if (!StringUtils.isEmpty(other.validationRelUrl)) {
            this.validationRelUrl = other.validationRelUrl;
        }

        if (!StringUtils.isEmpty(other.invitationLayout)) {
            this.invitationLayout = other.invitationLayout;
        }

        if (!StringUtils.isEmpty(other.listingLocalContentView)) {
            this.listingLocalContentView = other.listingLocalContentView;
        }
        
        if (!StringUtils.isEmpty(other.userInfoSchemaName)) {
            this.userInfoSchemaName = other.userInfoSchemaName;
        }
        
        if (!StringUtils.isEmpty(other.userInfoUsernameField)) {
            this.userInfoUsernameField = other.userInfoUsernameField;
        }
        
        if (!StringUtils.isEmpty(other.userInfoFirstnameField)) {
            this.userInfoFirstnameField = other.userInfoFirstnameField;
        }
        
        if (!StringUtils.isEmpty(other.userInfoLastnameField)) {
            this.userInfoLastnameField = other.userInfoLastnameField;
        }
        
        if (!StringUtils.isEmpty(other.userInfoEmailField)) {
            this.userInfoEmailField = other.userInfoEmailField;
        }
        
        if (!StringUtils.isEmpty(other.userInfoPasswordField)) {
            this.userInfoPasswordField = other.userInfoPasswordField;
        }
        
        
        if (!StringUtils.isEmpty(other.userInfoCompanyField)) {
            this.userInfoCompanyField = other.userInfoCompanyField;
        }
        
        if (!StringUtils.isEmpty(other.userInfoGroupsField)) {
            this.userInfoGroupsField = other.userInfoGroupsField;
        }
    }
}
