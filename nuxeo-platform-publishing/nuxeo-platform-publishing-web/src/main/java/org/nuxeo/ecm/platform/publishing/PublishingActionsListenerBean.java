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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: PublishingActionsListenerBean.java 28957 2008-01-11 13:36:52Z tdelprat $
 */

package org.nuxeo.ecm.platform.publishing;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.util.Map;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.publishing.api.PublishingException;
import org.nuxeo.ecm.platform.publishing.api.PublishingService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.webapp.base.InputController;

/**
 * Publishing actions listener. Listens to publish/reject document actions.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
@Name("publishingActions")
@Scope(CONVERSATION)
public class PublishingActionsListenerBean extends InputController implements
        ValidatorActionsService {
    private PublishingService publishingWorkflowFacade;

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected transient NuxeoPrincipal currentUser;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true)
    protected transient PublishActions publishActions;

    @In
    protected transient Context eventContext;

    @In(create = true)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    protected String rejectPublishingComment;

    protected Map<String, String> getMessages() {
        return messages;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.nuxeo.ecm.platform.publishing.ValidatorActionsService#publishDocument
     * ()
     */
    public String publishDocument() throws PublishingException {
        publishingWorkflowFacade.validatorPublishDocument(
                navigationContext.getCurrentDocument(), currentUser);
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.nuxeo.ecm.platform.publishing.ValidatorActionsService#rejectDocument
     * ()
     */
    public String rejectDocument() throws PublishingException {
        publishingWorkflowFacade.validatorRejectPublication(
                getCurrentDocument(), currentUser, rejectPublishingComment);
        try {
            return navigationContext.navigateToRef(getCurrentDocument().getParentRef());
        } catch (ClientException e) {
            throw new PublishingException(e);
        }
    }

    protected DocumentModel getCurrentDocument() {
        return navigationContext.getCurrentDocument();
    }

    public boolean isProxy() {
        boolean result = false;
        DocumentModel document = navigationContext.getCurrentContentRoot();
        if (document != null) {
            String parentDocumentType = document.getType();
            result = publishActions.getSectionRootTypes().contains(
                    parentDocumentType)
                    || publishActions.getSectionTypes().contains(
                            parentDocumentType);
        }
        return result;
    }

    public boolean canManagePublishing() throws PublishingException {
        return publishingWorkflowFacade.canManagePublishing(
                navigationContext.getCurrentDocument(), currentUser);
    }

    public String getRejectPublishingComment() {
        return rejectPublishingComment;
    }

    public void setRejectPublishingComment(String rejectPublishingComment) {
        this.rejectPublishingComment = rejectPublishingComment;
    }

}
