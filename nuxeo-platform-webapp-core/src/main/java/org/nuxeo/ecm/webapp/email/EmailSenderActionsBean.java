/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import javax.annotation.security.PermitAll;
import javax.ejb.Remove;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Renderer;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.security.PrincipalListManager;

/**
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@Name("emailSenderAction")
@Scope(STATELESS)
public class EmailSenderActionsBean extends InputController implements
        EmailSenderActions {

    private static final Log log = LogFactory.getLog(EmailSenderActionsBean.class);

    @In(create = true)
    UserManager userManager;

    @In(create = true)
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

    // @Create
    public void initialize() {
        log.info("Initializing...");
        log.info("Principal List Manager: " + principalListManager);
    }

    // @Destroy
    @Remove
    @PermitAll
    public void destroy() {
        log.debug("Removing Seam action listener...");
    }

    // @PrePassivate
    public void saveState() {
        log.info("PrePassivate");
    }

    // @PostActivate
    public void readState() {
        log.info("PostActivate");
    }

    public void send() {
        if (mailSubject == null || mailSubject.trim().length() == 0) {
            facesMessages.add(FacesMessage.SEVERITY_ERROR,
                    resourcesAccessor.getMessages().get(
                            "label.email.subject.empty"));
            return;
        }
        /*
         * if (mailContent == null || mailContent.trim().length() == 0){
         * facesMessages.add(FacesMessage.SEVERITY_ERROR, resourcesAccessor
         * .getMessages().get("label.email.content.empty")); return; }
         */
        if (principalListManager.getSelectedUserListEmpty()) {
            facesMessages.add(FacesMessage.SEVERITY_ERROR,
                    resourcesAccessor.getMessages().get(
                            "label.email.nousers.selected"));
        } else {
            NuxeoPrincipal currentUser = (NuxeoPrincipal) FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
            // XXX hack, principals have only one model
            DataModel dm = currentUser.getModel().getDataModels().values().iterator().next();
            fromEmail = (String) dm.getData(NuxeoPrincipalImpl.EMAIL_COLUMN); // XXX
            List<NuxeoPrincipal> listEmails = new ArrayList<NuxeoPrincipal>();
            for (String user : principalListManager.getSelectedUsers()) {
                try {
                    NuxeoPrincipal principal = userManager.getPrincipal(user);
                    listEmails.add(principal);
                } catch (ClientException e) {
                    continue;
                }
            }
            toEmail = listEmails;
            currentDocumentFullUrl = DocumentModelFunctions.documentUrl(navigationContext.getCurrentDocument());
            log.info("URL : "
                    + DocumentModelFunctions.documentUrl(navigationContext.getCurrentDocument()));

            try {
                log.info("Subject : " + mailSubject);
                log.info("Content : " + mailContent);
                renderer.render("/mail_template.xhtml");
                facesMessages.add(FacesMessage.SEVERITY_INFO,
                        resourcesAccessor.getMessages().get(
                                "label.email.send.ok"));
            } catch (Exception e) {
                facesMessages.add(FacesMessage.SEVERITY_ERROR,
                        resourcesAccessor.getMessages().get(
                                "label.email.send.failed"));
                log.error("Email sending failed:" + e.getMessage());
            }
        }
    }

    /**
     * @return the mailContent.
     */
    public String getMailContent() {
        return mailContent;
    }

    /**
     * @param mailContent the mailContent to set.
     */
    public void setMailContent(String mailContent) {
        this.mailContent = mailContent;
    }

    /**
     * @return the mailSubject.
     */
    public String getMailSubject() {
        return mailSubject;
    }

    /**
     * @param mailSubject the mailSubject to set.
     */
    public void setMailSubject(String mailSubject) {
        this.mailSubject = mailSubject;
    }

    /**
     * @return the principalListManager.
     */
    public PrincipalListManager getPrincipalListManager() {
        return principalListManager;
    }

    /**
     * @param principalListManager the principalListManager to set.
     */
    public void setPrincipalListManager(
            PrincipalListManager principalListManager) {
        this.principalListManager = principalListManager;
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

}
