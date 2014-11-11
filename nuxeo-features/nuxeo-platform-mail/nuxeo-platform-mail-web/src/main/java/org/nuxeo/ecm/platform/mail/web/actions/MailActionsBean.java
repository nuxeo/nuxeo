/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.mail.web.actions;

import static org.nuxeo.ecm.platform.mail.web.utils.MailWebConstants.CURRENT_PAGE;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.MAIL_FOLDER_TYPE;

import java.io.Serializable;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.ejb.SerializedConcurrentAccess;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.mail.utils.MailCoreHelper;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * Handles mail actions.
 *
 * @author Catalin Baican
 */
@Name("mailActions")
@Scope(ScopeType.CONVERSATION)
@SerializedConcurrentAccess
public class MailActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(MailActionsBean.class);

    @In(create = true, required = false)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    public String checkCurrentInbox() throws ClientException {
        DocumentModel mailFolder = navigationContext.getCurrentDocument();

        try {
            MailCoreHelper.checkMail(mailFolder, documentManager);
        } catch (Exception e) {
            log.debug(e, e);
            facesMessages.add(FacesMessage.SEVERITY_ERROR,
                    resourcesAccessor.getMessages().get(
                            "feedback.check.mail.error")
                            + e.getMessage());

            return CURRENT_PAGE;
        }

        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get(
                        "feedback.check.mail.success"));

        return CURRENT_PAGE;
    }

    public boolean isMailFolder() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return MAIL_FOLDER_TYPE.equals(currentDocument.getType());
    }

}
