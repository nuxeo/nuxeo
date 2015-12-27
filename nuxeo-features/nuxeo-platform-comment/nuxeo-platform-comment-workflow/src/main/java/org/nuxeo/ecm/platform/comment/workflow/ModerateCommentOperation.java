/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;
import org.nuxeo.ecm.platform.comment.workflow.utils.FollowTransitionUnrestricted;
import org.nuxeo.ecm.platform.task.Task;

/**
 * @since 5.5
 */
@Operation(id = ModerateCommentOperation.ID, category = Constants.CAT_DOCUMENT, label = "Follow publish or reject transition", description = "Follow publish if accept is true, reject otherwise.", addToStudio = false)
public class ModerateCommentOperation {

    public static final String ID = "Comment.Moderate";

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession session;

    @Param(name = "accept")
    protected Boolean accept;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) {
        moderate(doc.getRef());
        return doc;
    }

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentRef docRef) {
        moderate(docRef);
        return session.getDocument(docRef);
    }

    protected void moderate(DocumentRef docRef) {
        DocumentModel taskDoc = (DocumentModel) ctx.get(OperationTaskVariableName.taskDocument.name());
        Task task = taskDoc.getAdapter(Task.class);
        DocumentRef targetDocRef = new IdRef(task.getVariable(CommentsConstants.COMMENT_ID));
        FollowTransitionUnrestricted runner;
        if (accept) {
            runner = new FollowTransitionUnrestricted(session, targetDocRef,
                    CommentsConstants.TRANSITION_TO_PUBLISHED_STATE);
        } else {
            runner = new FollowTransitionUnrestricted(session, targetDocRef, CommentsConstants.REJECT_STATE);
        }
        runner.runUnrestricted();
    }

}
