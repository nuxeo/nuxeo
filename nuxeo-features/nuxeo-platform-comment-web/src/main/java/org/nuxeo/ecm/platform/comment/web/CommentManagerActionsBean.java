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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.comment.api.CommentableDocument;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
@Name("commentManagerActions")
@Scope(CONVERSATION)
public class CommentManagerActionsBean extends
        AbstractCommentManagerActionsBean implements Serializable {

    private static final Log log = LogFactory.getLog(CommentManagerActionsBean.class);

    private static final long serialVersionUID = 6994714264125958209L;

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
     * Retrieves the list of comment trees associated with a document and
     * constructs a flat list of comments associated with their depth (to easily
     * display them with indentation).
     */
    @Factory(value = "documentThreadedComments", scope = EVENT)
    public List<ThreadEntry> getCommentsAsThread() throws ClientException {
        return getCommentsAsThread(null);
    }

}
