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

import static org.jboss.seam.international.StatusMessage.Severity.ERROR;
import static org.jboss.seam.international.StatusMessage.Severity.INFO;
import static org.nuxeo.ecm.user.registration.UserRegistrationService.ValidationMethod.EMAIL;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.user.registration.DocumentRegistrationInfo;
import org.nuxeo.ecm.user.registration.UserRegistrationException;
import org.nuxeo.ecm.user.registration.UserRegistrationInfo;
import org.nuxeo.ecm.user.registration.UserRegistrationService;
import org.nuxeo.ecm.webapp.helpers.EventManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;

@Name("userRegistrationActions")
@Scope(ScopeType.CONVERSATION)
public class UserRegistrationActions implements Serializable {

    private static final long serialVersionUID = 53468164827894L;

    private static Log log = LogFactory.getLog(UserRegistrationActions.class);

    protected UserRegistrationService userRegistrationService;

    protected UserRegistrationInfo userinfo = new UserRegistrationInfo();

    protected DocumentRegistrationInfo docinfo = new DocumentRegistrationInfo();

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    public UserRegistrationInfo getUserinfo() {
        return userinfo;
    }

    public DocumentRegistrationInfo getDocinfo() {
        return docinfo;
    }

    // Tweak to use same widgets between listing and forms
    public UserRegistrationActions getData() {
        return this;
    }

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

    public void submitUserRegistration() {
        docinfo.setDocumentId(navigationContext.getCurrentDocument().getId());
        doSubmitUserRegistration();
    }

    protected void doSubmitUserRegistration() {
        try {
            getUserRegistrationService().submitRegistrationRequest(userinfo,
                    docinfo, getAdditionalsParameters(), EMAIL, false);

            facesMessages.add(
                    INFO,
                    resourcesAccessor.getMessages().get(
                            "label.user.invited.success"));
            resetPojos();
        } catch (ClientException e) {
            log.info("Unable to register user: " + e.getMessage());
            log.debug(e, e);
            facesMessages.add(
                    ERROR,
                    resourcesAccessor.getMessages().get(
                            "label.unable.invite.user"));
        }
    }

    protected Map<String, Serializable> getAdditionalsParameters() {
        return new HashMap<String, Serializable>();
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

    @Observer({ EventNames.DOCUMENT_CHANGED })
    public void resetPojos() {
        userinfo = new UserRegistrationInfo();
        docinfo = new DocumentRegistrationInfo();
    }
}
