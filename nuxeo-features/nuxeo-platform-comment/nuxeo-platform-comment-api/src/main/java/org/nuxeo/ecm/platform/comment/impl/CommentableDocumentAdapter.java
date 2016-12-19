/*
 * (C) Copyright 2007-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.api.CommentableDocument;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class CommentableDocumentAdapter implements CommentableDocument {

    private static final long serialVersionUID = 2996381735762615450L;

    final DocumentModel docModel;

    public CommentableDocumentAdapter(DocumentModel docModel) {
        this.docModel = docModel;
    }

    public DocumentModel addComment(DocumentModel comment) {
        CommentManager commentManager = Framework.getService(CommentManager.class);
        return commentManager.createComment(docModel, comment);
    }

    public DocumentModel addComment(DocumentModel parent, DocumentModel comment) {
        CommentManager commentManager = Framework.getService(CommentManager.class);
        return commentManager.createComment(docModel, parent, comment);
    }

    public void removeComment(DocumentModel comment) {
        CommentManager commentManager = Framework.getService(CommentManager.class);
        commentManager.deleteComment(docModel, comment);
    }

    public List<DocumentModel> getComments() {
        CommentManager commentManager = Framework.getService(CommentManager.class);
        return commentManager.getComments(docModel);
    }

    public List<DocumentModel> getComments(DocumentModel parent) {
        CommentManager commentManager = Framework.getService(CommentManager.class);
        return commentManager.getComments(docModel, parent);
    }

}
