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

package org.nuxeo.ecm.user.registration.actions;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.user.registration.UserRegistrationException;
import org.nuxeo.ecm.user.registration.UserRegistrationService;
import org.nuxeo.ecm.webapp.helpers.EventManager;
import org.nuxeo.runtime.api.Framework;

@Name("userRegistrationActions")
@Scope(ScopeType.CONVERSATION)
public class UserRegistrationActions {

    protected UserRegistrationService userRegistrationService;

    public String getDocType() throws ClientException {
        return getUserRegistrationService().getConfiguration().getRequestDocType();
    }

    public String getValidationBaseUrl() throws ClientException {
        return BaseURL.getBaseURL()
                + getUserRegistrationService().getConfiguration().getValidationRelUrl();
    }

    public void acceptRegistrationRequest(DocumentModel request)
            throws UserRegistrationException, ClientException {
        Map<String, Serializable> additionalInfo = new HashMap<String, Serializable>();
        additionalInfo.put("validationBaseURL", getValidationBaseUrl());
        getUserRegistrationService().acceptRegistrationRequest(request.getId(),
                additionalInfo);
        EventManager.raiseEventsOnDocumentChange(request);
    }

    public void rejectRegistrationRequest(DocumentModel request)
            throws UserRegistrationException, ClientException {
        Map<String, Serializable> additionalInfo = new HashMap<String, Serializable>();
        additionalInfo.put("validationBaseURL", getValidationBaseUrl());
        getUserRegistrationService().rejectRegistrationRequest(request.getId(),
                additionalInfo);
        EventManager.raiseEventsOnDocumentChange(request);
    }

    protected UserRegistrationService getUserRegistrationService()
            throws ClientException {
        if (userRegistrationService == null) {
            try {
                userRegistrationService = Framework.getService(UserRegistrationService.class);
            } catch (Exception e) {
                throw new ClientException(
                        "Failed to get UserRegistrationService", e);
            }
        }
        return userRegistrationService;
    }
}
