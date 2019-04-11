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

package org.nuxeo.ecm.webapp.email;

import static org.jboss.seam.ScopeType.STATELESS;

import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.Renderer;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.security.PrincipalListManager;

/**
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
@Name("emailSenderAction")
@Scope(STATELESS)
public class EmailSenderActionsBean extends InputController implements EmailSenderActions {

    private static final Log log = LogFactory.getLog(EmailSenderActionsBean.class);

    @In(create = true)
    UserManager userManager;

    @In(create = true, required = false)
    CoreSession documentManager;

    @In(create = true)
    private Renderer renderer;

    @In(required = false)
    @Out(required = false)
    private String mailSubject;

    @In(required = false)
    @Out(required = false)
    private String mailContent;

    @In(required = false)
    @Out(required = false)
    private String currentDocumentFullUrl;

    @In(create = true)
    @Out
    private PrincipalListManager principalListManager;

    @Out(required = false)
    private String fromEmail;

    @Out(required = false)
    private List<NuxeoPrincipal> toEmail;

    @Override
    public void send() {
        if (mailSubject == null || mailSubject.trim().length() == 0) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get("label.email.subject.empty"));
            return;
        }
        if (principalListManager.getSelectedUserListEmpty()) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get("label.email.nousers.selected"));
        } else {
            NuxeoPrincipal currentUser = (NuxeoPrincipal) FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
            fromEmail = currentUser.getEmail();
            List<NuxeoPrincipal> listEmails = new ArrayList<>();
            for (String user : principalListManager.getSelectedUsers()) {
                NuxeoPrincipal principal = userManager.getPrincipal(user);
                listEmails.add(principal);
            }
            toEmail = listEmails;
            currentDocumentFullUrl = DocumentModelFunctions.documentUrl(navigationContext.getCurrentDocument());
            log.debug("URL : " + DocumentModelFunctions.documentUrl(navigationContext.getCurrentDocument()));

            try {
                log.debug("Subject : " + mailSubject);
                log.debug("Content : " + mailContent);
                renderer.render("/mail_template.xhtml");
                facesMessages.add(StatusMessage.Severity.INFO,
                        resourcesAccessor.getMessages().get("label.email.send.ok"));
            } catch (RuntimeException e) { // stupid Seam FaceletsRenderer throws RuntimeException
                facesMessages.add(StatusMessage.Severity.ERROR,
                        resourcesAccessor.getMessages().get("label.email.send.failed"));
                log.error("Email sending failed:" + e.getMessage());
            }
        }
    }

    @Override
    public String getMailContent() {
        return mailContent;
    }

    @Override
    public void setMailContent(String mailContent) {
        this.mailContent = mailContent;
    }

    @Override
    public String getMailSubject() {
        return mailSubject;
    }

    @Override
    public void setMailSubject(String mailSubject) {
        this.mailSubject = mailSubject;
    }

    public PrincipalListManager getPrincipalListManager() {
        return principalListManager;
    }

    public void setPrincipalListManager(PrincipalListManager principalListManager) {
        this.principalListManager = principalListManager;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public List<NuxeoPrincipal> getToEmail() {
        return toEmail;
    }

    public void setToEmail(List<NuxeoPrincipal> toEmail) {
        this.toEmail = toEmail;
    }

    public String getCurrentDocumentFullUrl() {
        return currentDocumentFullUrl;
    }

    public void setCurrentDocumentFullUrl(String currentDocumentFullUrl) {
        this.currentDocumentFullUrl = currentDocumentFullUrl;
    }

}
