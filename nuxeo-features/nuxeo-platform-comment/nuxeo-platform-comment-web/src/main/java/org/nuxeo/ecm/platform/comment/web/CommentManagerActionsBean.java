/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.comment.web;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.comment.api.CommentableDocument;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
@Name("commentManagerActions")
@Scope(CONVERSATION)
public class CommentManagerActionsBean extends AbstractCommentManagerActionsBean implements Serializable {

    private static final Log log = LogFactory.getLog(CommentManagerActionsBean.class);

    private static final long serialVersionUID = 6994714264125958209L;

    @Override
    protected CommentableDocument getCommentableDoc() {
        if (commentableDoc == null) {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            if (currentDocument == null) {
                // what can I do?
                return null;
            }
            commentableDoc = currentDocument.getAdapter(CommentableDocument.class);
        }
        return commentableDoc;
    }

    /**
     * Retrieves the list of comment trees associated with a document and constructs a flat list of comments associated
     * with their depth (to easily display them with indentation).
     */
    @Override
    @Factory(value = "documentThreadedComments", scope = EVENT)
    public List<ThreadEntry> getCommentsAsThread() {
        return getCommentsAsThread(null);
    }

}
