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

package org.nuxeo.ecm.user.registration;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("configuration")
public class UserRegistrationConfiguration {

    @XNode("@merge")
    private boolean merge = false;

    @XNode("requestDocType")
    private String requestDocType;

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

    @XNode("registrationUserFactory")
    private Class<? extends RegistrationUserFactory> registrationUserFactory;

    @XNode("validationRelUrl")
    private String validationRelUrl;

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

    public Class<? extends RegistrationUserFactory> getRegistrationUserFactory() {
        return registrationUserFactory;
    }

    public String getValidationRelUrl() {
        return validationRelUrl;
    }

    public boolean isMerge() {
        return merge;
    }

    public void setMerge(boolean merge) {
        this.merge = merge;
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

        if (other.getRegistrationUserFactory() != null) {
            this.registrationUserFactory = other.registrationUserFactory;
        }

        if (!StringUtils.isEmpty(other.validationRelUrl)) {
            this.validationRelUrl = other.validationRelUrl;
        }
    }
}
