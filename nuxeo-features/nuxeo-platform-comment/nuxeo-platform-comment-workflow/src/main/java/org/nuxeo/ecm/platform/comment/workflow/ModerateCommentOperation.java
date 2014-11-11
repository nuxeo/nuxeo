/*
 * Copyright (c) 2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ldoguin
 */
package org.nuxeo.ecm.platform.comment.workflow;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.task.CreateTask.OperationTaskVariableName;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;
import org.nuxeo.ecm.platform.comment.workflow.utils.FollowTransitionUnrestricted;
import org.nuxeo.ecm.platform.task.Task;

/**
 *
 *@since 5.5
 */
@Operation(id = ModerateCommentOperation.ID, category = Constants.CAT_DOCUMENT, label = "Follow publish or reject transition", description = "Follow publish if accept is true, reject otherwise.")
public class ModerateCommentOperation {

    public static final String ID = "Comment.Moderate";

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession session;

    @Param(name = "accept")
    protected Boolean accept;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws Exception {
        moderate(doc.getRef());
        return doc;
    }

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentRef docRef) throws Exception {
        moderate(docRef);
        return session.getDocument(docRef);
    }

    protected void moderate(DocumentRef docRef) throws ClientException {
        DocumentModel taskDoc = (DocumentModel) ctx.get(OperationTaskVariableName.taskDocument.name());
        Task task = taskDoc.getAdapter(Task.class);
        DocumentRef targetDocRef = new IdRef(task.getVariable(CommentsConstants.COMMENT_ID));
        FollowTransitionUnrestricted runner;
        if (accept) {
            runner = new FollowTransitionUnrestricted(session, targetDocRef,
                    CommentsConstants.TRANSITION_TO_PUBLISHED_STATE);
        } else {
            runner = new FollowTransitionUnrestricted(session, targetDocRef,
                    CommentsConstants.REJECT_STATE);
        }
        runner.runUnrestricted();
    }

}
