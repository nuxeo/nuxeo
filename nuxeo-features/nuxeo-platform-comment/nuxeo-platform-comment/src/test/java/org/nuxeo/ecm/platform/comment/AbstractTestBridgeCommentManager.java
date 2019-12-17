/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.platform.comment;

import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_DOC_TYPE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_PARENT_ID;

import org.junit.Before;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.impl.BridgeCommentManager;
import org.nuxeo.ecm.platform.comment.impl.PropertyCommentManager;
import org.nuxeo.ecm.platform.comment.impl.TreeCommentManager;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @since 10.3
 */
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.relations.api")
@Deploy("org.nuxeo.ecm.relations")
@Deploy("org.nuxeo.ecm.relations.jena")
@Deploy("org.nuxeo.ecm.platform.comment.tests:OSGI-INF/comment-jena-contrib.xml")
public abstract class AbstractTestBridgeCommentManager extends AbstractTestCommentManager {

    protected static final String FILE_DOC_TYPE = "File";

    @Before
    public void before() {
        commentManager = getBridgeCommentManager();
    }

    protected DocumentModel createComment(CommentManager commentManager) {
        // Create the file to be commented
        DocumentModel domain = session.createDocumentModel("/", "test-domain", "Domain");
        session.createDocument(domain);
        DocumentModel fileToComment = session.createDocumentModel(domain.getPathAsString(), "anyFile", "File");
        fileToComment = session.createDocument(fileToComment);
        transactionalFeature.nextTransaction();

        // Add a comment
        DocumentModel commentDocModel = session.createDocumentModel(null, "Fake comment", COMMENT_DOC_TYPE);
        boolean setParent = commentManager instanceof PropertyCommentManager
                || commentManager instanceof TreeCommentManager;
        // Because we don't use the CommentableDocumentAdapter which will set this property, we should fill it here
        if (setParent) {
            commentDocModel.setPropertyValue(COMMENT_PARENT_ID, fileToComment.getId());
        }

        DocumentModel createdComment = commentManager.createComment(fileToComment, commentDocModel);
        transactionalFeature.nextTransaction();
        return session.getDocument(new IdRef(createdComment.getId()));
    }

    protected DocumentModel getCommentedDocument() {
        return session.query(String.format("SELECT * FROM Document Where %s = '%s'", NXQL.ECM_NAME, "anyFile")).get(0);
    }

    @Override
    public Class<? extends CommentManager> getType() {
        return BridgeCommentManager.class;
    }

    protected abstract BridgeCommentManager getBridgeCommentManager();
}
