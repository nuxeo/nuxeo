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
 * $Id: AbstractForumWorkflowDocumentHandler.java 28515 2008-01-06 20:37:29Z sfermigier $
 */

package org.nuxeo.ecm.platform.forum.workflow;

import org.jbpm.graph.exe.ExecutionContext;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.workflow.document.api.relation.WorkflowDocumentRelationManager;
import org.nuxeo.ecm.platform.workflow.jbpm.handlers.api.client.AbstractWorkflowDocumentActionHandler;
import org.nuxeo.ecm.platform.workflow.jbpm.util.IDConverter;

/**
 * This abc overrides the process to document ref binding for the post
 * moderation workflow.
 *
 * <p>
 * Here, we'd like to bind the process not with the post itself but with the
 * thread since the post is right now an hidden object (i.e : comment). Like
 * this, while getting a workitem, the document ref will be the thread and one
 * will be able to access it from dashboard. The rest of the API won't be
 * changed since the security and the life cycle will be updated on the post
 * itself.
 * </p>
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public abstract class AbstractForumWorkflowDocumentHandler extends
        AbstractWorkflowDocumentActionHandler {

    protected AbstractForumWorkflowDocumentHandler() {
    }

    @Override
    protected void bindDocumentToProcess(ExecutionContext ec) throws Exception {
        WorkflowDocumentRelationManager docRelManager = getWorkflowDocumentRelation();
        String pid = IDConverter.getNXWorkflowIdentifier(getProcessInstance(ec).getId());
        DocumentRef docRef = getThreadRef(ec); // override is here.
        if (docRef != null) {
            docRelManager.createDocumentWorkflowRef(docRef, pid);
        } else {
            log.error("Cannot bind document to process..doc ref not found");
        }
    }

    @Override
    protected void unbindDocumentToProcess(ExecutionContext ec)
            throws Exception {
        WorkflowDocumentRelationManager docRelManager = getWorkflowDocumentRelation();
        String pid = IDConverter.getNXWorkflowIdentifier(getProcessInstance(ec).getId());
        DocumentRef docRef = getThreadRef(ec); // override is here.
        if (docRef != null) {
            docRelManager.deleteDocumentWorkflowRef(docRef, pid);
        } else {
            log.error("Cannot bind document to process..doc ref not found");
        }
    }

    protected DocumentRef getThreadRef(ExecutionContext ec) {
        return (DocumentRef) ec.getVariable(ForumConstants.THREAD_REF);
    }

}
