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

import java.io.Serializable;
import java.util.Map;

import javax.faces.application.FacesMessage;

import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.publishing.api.Publisher;
import org.nuxeo.ecm.platform.publishing.api.PublishingException;
import org.nuxeo.ecm.platform.publishing.api.PublishingService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.runtime.api.Framework;

/**
 * Publishing actions listener. Listens to publish/reject document actions.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
@Name("publishingActions")
@Scope(CONVERSATION)
public class PublishingActionsListenerBean extends InputController implements
        ValidatorActionsService, Serializable {

    private PublishingService publishingService;

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
    protected transient Map<String, String> messages;

    protected String rejectPublishingComment;

    protected Map<String, String> getMessages() {
        return messages;
    }

    @Create
    public void create() {
        try {
            publishingService = Framework.getService(PublishingService.class);
        } catch (Exception e) {
            throw new IllegalStateException("Publishing service not deployed.",
                    e);
        }
    }

    public String publishDocument() throws PublishingException {
        publishingService.validatorPublishDocument(
                navigationContext.getCurrentDocument(), currentUser);
        Events.instance().raiseEvent(Publisher.PublishingEvent.documentPublished.name());
        return null;
    }

    public String rejectDocument() throws PublishingException {
        if (rejectPublishingComment == null
                || "".equals(rejectPublishingComment)) {
            facesMessages.addToControl("rejectPublishingComment",
                    FacesMessage.SEVERITY_ERROR,
                    resourcesAccessor.getMessages().get(
                            "label.publishing.reject.user.comment.mandatory"));
            return null;
        }

        publishingService.validatorRejectPublication(getCurrentDocument(),
                currentUser, rejectPublishingComment);
        Events.instance().raiseEvent(Publisher.PublishingEvent.documentPublicationRejected.name());
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
        return publishingService.canManagePublishing(
                navigationContext.getCurrentDocument(), currentUser);
    }

    public String getRejectPublishingComment() {
        return rejectPublishingComment;
    }

    public void setRejectPublishingComment(String rejectPublishingComment) {
        this.rejectPublishingComment = rejectPublishingComment;
    }

}
