/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.notification.email;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 */

@Name("emailNotifSenderAction")
@Scope(EVENT)
public class EmailNotificationSenderActionsBean extends InputController implements EmailNotificationSenderActions,
        Serializable {

    private static final long serialVersionUID = 2125646683248052737L;

    @In(create = true)
    transient UserManager userManager;

    @In(create = true, required = false)
    transient CoreSession documentManager;

    @In(required = false)
    @Out(required = false)
    private String mailSubject;

    @In(required = false)
    @Out(required = false)
    private String mailContent;

    @In(required = false)
    @Out(required = false)
    private String currentDocumentFullUrl;

    @Out(required = false)
    private String fromEmail;

    @Out(required = false)
    private List<NuxeoPrincipal> toEmail;

    private List<String> recipients;

    @Override
    public String send() {
        if (mailSubject == null || mailSubject.trim().length() == 0) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get("label.email.subject.empty"));
            return null;
        }
        /*
         * if (mailContent == null || mailContent.trim().length() == 0){ facesMessages.add(FacesMessage.SEVERITY_ERROR,
         * resourcesAccessor .getMessages().get("label.email.content.empty")); return; }
         */
        if (recipients == null || recipients.isEmpty()) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get("label.email.nousers.selected"));
            return null;
        }
        for (String user : recipients) {
            sendNotificationEvent(user, mailSubject, mailContent);
        }
        facesMessages.add(StatusMessage.Severity.INFO, resourcesAccessor.getMessages().get("label.email.send.ok"));

        // redirect to currentDocument default view
        DocumentModel cDoc = navigationContext.getCurrentDocument();
        if (cDoc == null) {
            return null;
        } else {
            TypeInfo typeInfo = cDoc.getAdapter(TypeInfo.class);
            if (typeInfo != null) {
                return typeInfo.getDefaultView();
            } else {
                return null;
            }
        }
    }

    /**
     * Sends an event that triggers a notification that sends emails to all selected entities.
     *
     * @param user
     * @param theMailSubject
     * @param theMailContent
     */
    private void sendNotificationEvent(String recipient, String theMailSubject, String theMailContent)
            {

        Map<String, Serializable> options = new HashMap<>();

        // options for confirmation email
        options.put(NotificationConstants.RECIPIENTS_KEY, new String[] { recipient });
        options.put("mailSubject", theMailSubject);
        options.put("mailContent", theMailContent);
        options.put("category", DocumentEventCategories.EVENT_CLIENT_NOTIF_CATEGORY);

        NuxeoPrincipal currentUser = (NuxeoPrincipal) FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();

        DocumentEventContext ctx = new DocumentEventContext(documentManager, currentUser,
                navigationContext.getCurrentDocument());
        ctx.setProperties(options);
        Event event = ctx.newEvent(DocumentEventTypes.EMAIL_DOCUMENT_SEND);

        EventProducer evtProducer = Framework.getService(EventProducer.class);
        evtProducer.fireEvent(event);

    }

    /**
     * @return the mailContent.
     */
    @Override
    public String getMailContent() {
        return mailContent;
    }

    /**
     * @param mailContent the mailContent to set.
     */
    @Override
    public void setMailContent(String mailContent) {
        this.mailContent = mailContent;
    }

    /**
     * @return the mailSubject.
     */
    @Override
    public String getMailSubject() {
        return mailSubject;
    }

    /**
     * @param mailSubject the mailSubject to set.
     */
    @Override
    public void setMailSubject(String mailSubject) {
        this.mailSubject = mailSubject;
    }

    /**
     * @return the fromEmail.
     */
    public String getFromEmail() {
        return fromEmail;
    }

    /**
     * @param fromEmail the fromEmail to set.
     */
    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    /**
     * @return the toEmail.
     */
    public List<NuxeoPrincipal> getToEmail() {
        return toEmail;
    }

    /**
     * @param toEmail the toEmail to set.
     */
    public void setToEmail(List<NuxeoPrincipal> toEmail) {
        this.toEmail = toEmail;
    }

    public String getCurrentDocumentFullUrl() {
        return currentDocumentFullUrl;
    }

    public void setCurrentDocumentFullUrl(String currentDocumentFullUrl) {
        this.currentDocumentFullUrl = currentDocumentFullUrl;
    }

    /**
     * @since 7.1
     */
    public List<String> getRecipients() {
        return recipients;
    }

    /**
     * @since 7.1
     */
    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }

}
