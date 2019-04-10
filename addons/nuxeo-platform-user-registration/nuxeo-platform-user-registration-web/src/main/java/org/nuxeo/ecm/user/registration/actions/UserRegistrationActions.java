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
import static org.nuxeo.ecm.user.registration.UserRegistrationConfiguration.DEFAULT_CONFIGURATION_NAME;
import static org.nuxeo.ecm.user.registration.UserRegistrationService.ValidationMethod.EMAIL;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.user.registration.DocumentRegistrationInfo;
import org.nuxeo.ecm.user.registration.UserRegistrationException;
import org.nuxeo.ecm.user.registration.UserRegistrationInfo;
import org.nuxeo.ecm.user.registration.UserRegistrationService;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

@Name("userRegistrationActions")
@Scope(ScopeType.CONVERSATION)
public class UserRegistrationActions implements Serializable {

    private static final long serialVersionUID = 53468164827894L;

    private static Log log = LogFactory.getLog(UserRegistrationActions.class);

    public static final String MULTIPLE_EMAILS_SEPARATOR = ";";

    public static final String REQUEST_DOCUMENT_LIST = "CURRENT_USER_REQUESTS";

    public static final String REQUESTS_DOCUMENT_LIST_CHANGED = "requestDocumentsChanged";

    protected UserRegistrationInfo userinfo = new UserRegistrationInfo();

    protected DocumentRegistrationInfo docinfo = new DocumentRegistrationInfo();

    protected String multipleEmails;

    protected String comment;

    protected boolean copyOwner = false;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    @In(create = true)
    protected transient DocumentsListsManager documentsListsManager;

    @In(create = true)
    protected transient ContentViewActions contentViewActions;

    @In(create = true)
    protected transient UserRegistrationService userRegistrationService;

    public UserRegistrationInfo getUserinfo() {
        return userinfo;
    }

    public DocumentRegistrationInfo getDocinfo() {
        return docinfo;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isCopyOwner() {
        return copyOwner;
    }

    public void setCopyOwner(boolean copyOwner) {
        this.copyOwner = copyOwner;
    }

    // Tweak to use same widgets between listing and forms
    public UserRegistrationActions getData() {
        return this;
    }

    public String getDocType() throws ClientException {
        return getDocType(DEFAULT_CONFIGURATION_NAME);
    }

    public String getDocType(String name) throws ClientException {
        return userRegistrationService.getConfiguration(name).getRequestDocType();
    }

    public String getValidationBaseUrl(String name) throws ClientException {
        return BaseURL.getBaseURL()
                + userRegistrationService.getConfiguration(name).getValidationRelUrl();
    }

    public String getInvitationLayout(String name) {
        return userRegistrationService.getConfiguration(name).getInvitationLayout();
    }

    public String getListingLocalContentView(String name) {
        return userRegistrationService.getConfiguration(name).getListingLocalContentView();
    }

    public String getMultipleEmails() {
        return multipleEmails;
    }

    public void setMultipleEmails(String multipleEmails) {
        this.multipleEmails = multipleEmails;
    }

    public String getValidationBaseUrl() throws ClientException {
        return getValidationBaseUrl(DEFAULT_CONFIGURATION_NAME);
    }

    public void acceptRegistrationRequest(DocumentModel request)
            throws UserRegistrationException, ClientException {
        Map<String, Serializable> additionalInfo = new HashMap<String, Serializable>();
        additionalInfo.put("validationBaseURL", getValidationBaseUrl());
        userRegistrationService.acceptRegistrationRequest(request.getId(),
                additionalInfo);
        // EventManager.raiseEventsOnDocumentChange(request);
        Events.instance().raiseEvent(REQUESTS_DOCUMENT_LIST_CHANGED);
    }

    public void rejectRegistrationRequest(DocumentModel request)
            throws UserRegistrationException, ClientException {
        Map<String, Serializable> additionalInfo = new HashMap<String, Serializable>();
        additionalInfo.put("validationBaseURL", getValidationBaseUrl());
        userRegistrationService.rejectRegistrationRequest(request.getId(),
                additionalInfo);
        // EventManager.raiseEventsOnDocumentChange(request);
        Events.instance().raiseEvent(REQUESTS_DOCUMENT_LIST_CHANGED);
    }

    public void submitUserRegistration(String configurationName) throws ClientException {
        docinfo.setDocumentId(navigationContext.getCurrentDocument().getId());
        docinfo.setDocumentTitle(navigationContext.getCurrentDocument().getTitle());
        doSubmitUserRegistration(configurationName);
        resetPojos();
        Events.instance().raiseEvent(REQUESTS_DOCUMENT_LIST_CHANGED);
    }

    public void submitMultipleUserRegistration(String configurationName) {
        if (StringUtils.isBlank(multipleEmails)) {
            facesMessages.add(
                    ERROR,
                    resourcesAccessor.getMessages().get(
                            "label.registration.multiple.empty"));
            return;
        }
        docinfo.setDocumentId(navigationContext.getCurrentDocument().getId());

        String[] emails = multipleEmails.split(MULTIPLE_EMAILS_SEPARATOR);
        for (String email : emails) {
            userinfo.setLogin(email.trim());
            userinfo.setEmail(email.trim());

            log.debug("Request email: " + email + " with multiple invitation.");
            doSubmitUserRegistration(configurationName);
        }
        resetPojos();
        Events.instance().raiseEvent(REQUESTS_DOCUMENT_LIST_CHANGED);
    }

    public boolean getCanValidate() {
        boolean canDelete = !documentsListsManager.isWorkingListEmpty(REQUEST_DOCUMENT_LIST);
        for (DocumentModel doc : documentsListsManager.getWorkingList(REQUEST_DOCUMENT_LIST)) {
            canDelete &= isDocumentValidable(doc);
        }
        return canDelete;
    }

    protected boolean isDocumentValidable(DocumentModel doc) {
        try {
            return "accepted".equals(doc.getCurrentLifeCycleState());
        } catch (ClientException e) {
            log.warn("Unable to get lifecycle state for " + doc.getId() + ": "
                    + e.getMessage());
            log.debug(e);
            return false;
        }
    }

    public boolean getCanDelete() {
        boolean canDelete = !documentsListsManager.isWorkingListEmpty(REQUEST_DOCUMENT_LIST);
        for (DocumentModel doc : documentsListsManager.getWorkingList(REQUEST_DOCUMENT_LIST)) {
            canDelete &= isDocumentDeletable(doc);
        }
        return canDelete;
    }

    protected boolean isDocumentDeletable(DocumentModel doc) {
        try {
            return !"validated".equals(doc.getCurrentLifeCycleState());
        } catch (ClientException e) {
            log.warn("Unable to get lifecycle state for " + doc.getId() + ": "
                    + e.getMessage());
            log.debug(e);
            return false;
        }
    }

    public boolean getCanRevive() {
        boolean canRevive = !documentsListsManager.isWorkingListEmpty(REQUEST_DOCUMENT_LIST);
        for (DocumentModel doc : documentsListsManager.getWorkingList(REQUEST_DOCUMENT_LIST)) {
            canRevive &= isDocumentRevivable(doc);
        }
        return canRevive;
    }

    protected boolean isDocumentRevivable(DocumentModel doc) {
        try {
            return "accepted".equals(doc.getCurrentLifeCycleState());
        } catch (ClientException e) {
            log.warn("Unable to get lifecycle state for " + doc.getId() + ": "
                    + e.getMessage());
            log.info(e);
            return false;
        }
    }

    public void validateUserRegistration() {
        if (!documentsListsManager.isWorkingListEmpty(REQUEST_DOCUMENT_LIST)) {
            try {
                for (DocumentModel registration : documentsListsManager.getWorkingList(REQUEST_DOCUMENT_LIST)) {
                    userRegistrationService.validateRegistration(
                            registration.getId(),
                            new HashMap<String, Serializable>());
                }
                Events.instance().raiseEvent(REQUESTS_DOCUMENT_LIST_CHANGED);
                facesMessages.add(
                        INFO,
                        resourcesAccessor.getMessages().get(
                                "label.validate.request"));
                documentsListsManager.resetWorkingList(REQUEST_DOCUMENT_LIST);
            } catch (ClientException e) {
                log.warn("Unable to validate registration: " + e.getMessage());
                log.info(e);
                facesMessages.add(
                        ERROR,
                        resourcesAccessor.getMessages().get(
                                "label.unable.validate.request"));
            }
        }
    }

    public void reviveUserRegistration() {
        if (!documentsListsManager.isWorkingListEmpty(REQUEST_DOCUMENT_LIST)) {
            try {
                userRegistrationService.reviveRegistrationRequests(
                        documentManager,
                        documentsListsManager.getWorkingList(REQUEST_DOCUMENT_LIST));
                Events.instance().raiseEvent(REQUESTS_DOCUMENT_LIST_CHANGED);
                facesMessages.add(
                        INFO,
                        resourcesAccessor.getMessages().get(
                                "label.revive.request"));
                documentsListsManager.resetWorkingList(REQUEST_DOCUMENT_LIST);
            } catch (ClientException e) {
                log.warn("Unable to revive user: " + e.getMessage());
                log.info(e);
                facesMessages.add(
                        ERROR,
                        resourcesAccessor.getMessages().get(
                                "label.unable.revive.request"));
            }
        }
    }

    public void deleteUserRegistration() {
        if (!documentsListsManager.isWorkingListEmpty(REQUEST_DOCUMENT_LIST)) {
            try {
                userRegistrationService.deleteRegistrationRequests(
                        documentManager,
                        documentsListsManager.getWorkingList(REQUEST_DOCUMENT_LIST));
                Events.instance().raiseEvent(REQUESTS_DOCUMENT_LIST_CHANGED);
                facesMessages.add(
                        INFO,
                        resourcesAccessor.getMessages().get(
                                "label.delete.request"));
                documentsListsManager.resetWorkingList(REQUEST_DOCUMENT_LIST);
            } catch (ClientException e) {
                log.warn("Unable to delete user request:" + e.getMessage());
                log.info(e);
                facesMessages.add(
                        ERROR,
                        resourcesAccessor.getMessages().get(
                                "label.unable.delete.request"));
            }

        }
    }

    protected void doSubmitUserRegistration(String configurationName) {
        if (StringUtils.isBlank(configurationName)) {
            configurationName = DEFAULT_CONFIGURATION_NAME;
        }

        try {
            userinfo.setPassword(RandomStringUtils.randomAlphanumeric(6));
            userRegistrationService.submitRegistrationRequest(
                    configurationName, userinfo, docinfo,
                    getAdditionalsParameters(), EMAIL, false);

            facesMessages.add(
                    INFO,
                    resourcesAccessor.getMessages().get(
                            "label.user.invited.success"));
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
        Map<String, Serializable> additionalsInfo = new HashMap<String, Serializable>();
        try {
            additionalsInfo.put("docinfo:documentTitle",
                    navigationContext.getCurrentDocument().getTitle());
            additionalsInfo.put(
                    "registration:copyTo",
                    ((NuxeoPrincipal) documentManager.getPrincipal()).getEmail());
            additionalsInfo.put("registration:comment", comment);
        } catch (ClientException e) {
            // log it silently as it will break anything
            log.debug(e, e);
        }
        return additionalsInfo;
    }

    @Observer({ EventNames.DOCUMENT_CHANGED })
    public void resetPojos() {
        userinfo = new UserRegistrationInfo();
        docinfo = new DocumentRegistrationInfo();
        multipleEmails = "";
        copyOwner = false;
        comment = "";
    }

    @Observer({ REQUESTS_DOCUMENT_LIST_CHANGED })
    public void refreshContentViewCache() {
        contentViewActions.refreshOnSeamEvent(REQUESTS_DOCUMENT_LIST_CHANGED);
        contentViewActions.resetPageProviderOnSeamEvent(REQUESTS_DOCUMENT_LIST_CHANGED);
    }
}
