/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.jbpm.core.helper;

import java.io.Serializable;
import java.util.List;

import org.jbpm.graph.exe.Comment;
import org.jbpm.graph.exe.ExecutionContext;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.jbpm.AbstractJbpmHandlerHelper;
import org.nuxeo.ecm.platform.jbpm.JbpmEventNames;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.VirtualTaskInstance;
import org.nuxeo.runtime.api.Framework;

/**
 * @author arussel
 */
public class TaskNotificationHandler extends AbstractJbpmHandlerHelper {

    private static final long serialVersionUID = 1L;

    private static final String DUE_DATE_KEY = "dueDate";

    private static final String DIRECTIVE_KEY = "directive";

    @Override
    public void execute(ExecutionContext executionContext) throws Exception {
        this.executionContext = executionContext;
        if (nuxeoHasStarted()) {
            DocumentModel documentModel = (DocumentModel) getTransientVariable(JbpmService.VariableName.document.name());
            NuxeoPrincipal principal = (NuxeoPrincipal) getTransientVariable(JbpmService.VariableName.principal.name());
            if (documentModel == null) {
                return;
            }

            CoreSession coreSession = getCoreSession(principal);
            try {
                EventProducer eventProducer;
                try {
                    eventProducer = Framework.getService(EventProducer.class);
                } catch (Exception e) {
                    throw new ClientException(e);
                }
                DocumentEventContext ctx = new DocumentEventContext(
                        coreSession, principal, documentModel);
                ctx.setProperty(NotificationConstants.RECIPIENTS_KEY,
                        getRecipients());
                ctx.setProperty(DocumentEventContext.COMMENT_PROPERTY_KEY,
                        getComments());
                ctx.setProperty(DUE_DATE_KEY,
                        executionContext.getTaskInstance().getDueDate());
                ctx.setProperty(
                        DIRECTIVE_KEY,
                        (Serializable) executionContext.getTaskInstance().getVariable(
                                "directive"));
                eventProducer.fireEvent(ctx.newEvent(JbpmEventNames.WORKFLOW_TASK_ASSIGNED));
            } finally {
                closeCoreSession(coreSession);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected String getComments() {
        List<Comment> comments = executionContext.getTaskInstance().getComments();
        return comments == null ? null
                : comments.get(comments.size() - 1).getMessage();
    }

    protected String[] getRecipients() {
        VirtualTaskInstance participant = (VirtualTaskInstance) getTransientVariable(JbpmService.VariableName.participant.name());
        if (participant == null) {
            participant = (VirtualTaskInstance) executionContext.getContextInstance().getVariable(
                    JbpmService.VariableName.participant.name());
        }
        return participant.getActors().toArray(new String[] {});
    }
}
