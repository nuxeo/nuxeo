/*
 * (C) Copyright 2007-2020 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.comment.impl;

import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_PARENT_ID;

import java.util.List;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.api.CommentableDocument;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class CommentableDocumentAdapter implements CommentableDocument {

    private static final long serialVersionUID = 2996381735762615450L;

    protected final DocumentModel docModel;

    protected final CommentManager commentManager;

    public CommentableDocumentAdapter(DocumentModel docModel) {
        this.commentManager = Framework.getService(CommentManager.class);
        this.docModel = docModel;
    }

    @Override
    public DocumentModel addComment(DocumentModel comment) {
        return addComment(docModel, comment);
    }

    @Override
    public DocumentModel addComment(DocumentModel comment, String path) {
        comment.setPropertyValue(COMMENT_PARENT_ID, docModel.getId());
        return commentManager.createLocatedComment(docModel, comment, path);
    }

    @Override
    public DocumentModel addComment(DocumentModel parent, DocumentModel comment) {
        comment.setPropertyValue(COMMENT_PARENT_ID, parent.getId());
        return commentManager.createComment(parent, comment);
    }

    @Override
    public void removeComment(DocumentModel comment) {
        commentManager.deleteComment(docModel.getCoreSession(), comment.getId());
    }

    @Override
    public List<DocumentModel> getComments() {
        return getComments(docModel);
    }

    @Override
    public List<DocumentModel> getComments(DocumentModel parent) {
        CoreSession session = docModel.getCoreSession();
        List<Comment> comments = commentManager.getComments(session, parent.getId());
        return CoreInstance.doPrivileged(session, s -> {
            return comments.stream().map(comment -> {
                DocumentModel commentModel = s.getDocument(new IdRef(comment.getId()));
                commentModel.detach(true);
                return commentModel;
            }).collect(Collectors.toList());
        });
    }

}
