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
import static org.nuxeo.ecm.user.invite.UserInvitationService.ValidationMethod.EMAIL;
import static org.nuxeo.ecm.user.registration.UserRegistrationService.CONFIGURATION_NAME;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

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
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.user.invite.UserRegistrationInfo;
import org.nuxeo.ecm.user.registration.DocumentRegistrationInfo;
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
        return getDocType(CONFIGURATION_NAME);
    }

    public String getDocType(String name) throws ClientException {
        return userRegistrationService.getConfiguration(name).getRequestDocType();
    }

    public String getValidationBaseUrl(String name) throws ClientException {
        return BaseURL.getBaseURL()
                + userRegistrationService.getConfiguration(name).getValidationRelUrl();
    }

    public String getEnterPasswordUrl(String name) throws ClientException {
        return BaseURL.getBaseURL()
                + userRegistrationService.getConfiguration(name).getEnterPasswordUrl();
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
        return getValidationBaseUrl(CONFIGURATION_NAME);
    }

    public String getEnterPasswordUrl() throws ClientException {
        return getEnterPasswordUrl(CONFIGURATION_NAME);
    }

    public void acceptRegistrationRequest(DocumentModel request) {
        try {
            Map<String, Serializable> additionalInfo = new HashMap<String, Serializable>();
            additionalInfo.put("enterPasswordUrl", getEnterPasswordUrl());
            // Determine the document url to add it into the email
            String docId = (String) request.getPropertyValue(DocumentRegistrationInfo.DOCUMENT_ID_FIELD);
            DocumentRef docRef = new IdRef(docId);
            DocumentModel doc = documentManager.getDocument(docRef);
            String docUrl = DocumentModelFunctions.documentUrl(doc);
            additionalInfo.put("docUrl", docUrl);

            userRegistrationService.acceptRegistrationRequest(request.getId(),
                    additionalInfo);

            // EventManager.raiseEventsOnDocumentChange(request);
            Events.instance().raiseEvent(REQUESTS_DOCUMENT_LIST_CHANGED);
        } catch (ClientException e) {
            facesMessages.add(ERROR, e.getMessage());
        }
    }

    public void rejectRegistrationRequest(DocumentModel request) {
        try {
            Map<String, Serializable> additionalInfo = new HashMap<String, Serializable>();
            additionalInfo.put("validationBaseURL", getValidationBaseUrl());
            userRegistrationService.rejectRegistrationRequest(request.getId(),
                    additionalInfo);
            // EventManager.raiseEventsOnDocumentChange(request);
            Events.instance().raiseEvent(REQUESTS_DOCUMENT_LIST_CHANGED);
        } catch (ClientException e) {
            facesMessages.add(ERROR, e.getMessage());
        }
    }

    public void submitUserRegistration(String configurationName) {
        try {
            docinfo.setDocumentId(navigationContext.getCurrentDocument().getId());
            docinfo.setDocumentTitle(navigationContext.getCurrentDocument().getTitle());
            doSubmitUserRegistration(configurationName);
            resetPojos();
            Events.instance().raiseEvent(REQUESTS_DOCUMENT_LIST_CHANGED);
        } catch (ClientException e) {
            facesMessages.add(ERROR, e.getMessage());
        }
    }

    public void submitMultipleUserRegistration(String configurationName)
            throws AddressException {
        if (StringUtils.isBlank(multipleEmails)) {
            facesMessages.add(
                    ERROR,
                    resourcesAccessor.getMessages().get(
                            "label.registration.multiple.empty"));
            return;
        }
        docinfo.setDocumentId(navigationContext.getCurrentDocument().getId());

        InternetAddress[] emails = splitAddresses(multipleEmails);
        for (InternetAddress email : emails) {
            userinfo.setLogin(email.getAddress());
            userinfo.setEmail(email.getAddress());

            log.debug("Request email: " + email + " with multiple invitation.");
            doSubmitUserRegistration(configurationName);
        }
        resetPojos();
        Events.instance().raiseEvent(REQUESTS_DOCUMENT_LIST_CHANGED);
    }

    protected InternetAddress[] splitAddresses(String emails)
            throws AddressException {
        return StringUtils.isNotBlank(emails) ? InternetAddress.parse(
                emails.replace(MULTIPLE_EMAILS_SEPARATOR, ","), false)
                : new InternetAddress[] {};
    }

    public void validateMultipleUser(FacesContext context,
            UIComponent component, Object value) {
        if (value instanceof String) {
            try {
                splitAddresses((String) value);
                return;
            } catch (AddressException e) {
                // Nothing to do, error is handled after
            }
        }

        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                ComponentUtils.translate(context,
                        "label.request.error.multiple.emails"), null);

        // also add global message
        context.addMessage(null, message);
        throw new ValidatorException(message);
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
            configurationName = CONFIGURATION_NAME;
        }

        try {
            userRegistrationService.submitRegistrationRequest(
                    configurationName, userinfo, docinfo,
                    getAdditionalsParameters(), EMAIL, false, documentManager.getPrincipal().getName());

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
            if (copyOwner) {
                additionalsInfo.put(
                        "registration:copyTo",
                        ((NuxeoPrincipal) documentManager.getPrincipal()).getEmail());
            }
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
