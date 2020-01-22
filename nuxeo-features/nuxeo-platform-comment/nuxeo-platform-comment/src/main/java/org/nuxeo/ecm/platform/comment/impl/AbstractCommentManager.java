/*
 * (C) Copyright 2018-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuno Cunha <ncunha@nuxeo.com>
 *     Nour AL KOTOB
 */

package org.nuxeo.ecm.platform.comment.impl;

import static java.util.Objects.requireNonNull;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_AUTHOR;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_PARENT_ID;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_SCHEMA;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentConstants;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 10.3
 */
public abstract class AbstractCommentManager implements CommentManager {

    private static final Logger log = LogManager.getLogger(AbstractCommentManager.class);

    protected static final String COMMENTS_DIRECTORY = "Comments";

    @Override
    public List<DocumentModel> getComments(DocumentModel docModel) {
        return getComments(docModel.getCoreSession(), docModel);
    }

    @Override
    public List<DocumentModel> getComments(DocumentModel docModel, DocumentModel parent) {
        return getComments(docModel);
    }

    @Override
    public List<Comment> getComments(CoreSession session, String documentId) {
        return getComments(session, documentId, 0L, 0L, true);
    }

    @Override
    public List<Comment> getComments(CoreSession session, String documentId, boolean sortAscending) {
        return getComments(session, documentId, 0L, 0L, sortAscending);
    }

    @Override
    public PartialList<Comment> getComments(CoreSession session, String documentId, Long pageSize,
            Long currentPageIndex) {
        return getComments(session, documentId, pageSize, currentPageIndex, true);
    }

    @Override
    public DocumentModel getThreadForComment(DocumentModel comment) {
        DocumentRef topLevelDocRef = getTopLevelCommentAncestor(comment.getCoreSession(), comment.getRef());
        return comment.getCoreSession().getDocument(topLevelDocRef);
    }

    protected void notifyEvent(CoreSession session, String eventType, DocumentModel commentedDoc,
            DocumentModel comment) {

        UserManager userManager = Framework.getService(UserManager.class);
        NuxeoPrincipal principal = null;
        if (userManager != null) {
            principal = userManager.getPrincipal((String) comment.getPropertyValue(COMMENT_AUTHOR));
            if (principal == null) {
                try {
                    principal = getAuthor(comment);
                } catch (PropertyException e) {
                    log.error("Error building principal for comment author", e);
                    return;
                }
            }
        }
        DocumentRef topLevelDocRef = getTopLevelCommentAncestor(session, commentedDoc.getRef());
        DocumentModel topLevelDocument = session.getDocument(topLevelDocRef);
        DocumentEventContext ctx = new DocumentEventContext(session, principal, commentedDoc);
        Map<String, Serializable> props = new HashMap<>();
        props.put(CommentConstants.TOP_LEVEL_DOCUMENT, topLevelDocument);
        props.put(CommentConstants.PARENT_COMMENT, commentedDoc);
        props.put(CommentConstants.COMMENT_DOCUMENT, comment);
        props.put(CommentConstants.COMMENT, (String) comment.getProperty("comment", "text"));
        // Keep comment_text for compatibility
        props.put(CommentConstants.COMMENT_TEXT, (String) comment.getProperty("comment", "text"));
        props.put("category", CommentConstants.EVENT_COMMENT_CATEGORY);
        ctx.setProperties(props);
        Event event = ctx.newEvent(eventType);

        EventProducer evtProducer = Framework.getService(EventProducer.class);
        evtProducer.fireEvent(event);
    }

    protected NuxeoPrincipal getAuthor(DocumentModel docModel) {
        String[] contributors = (String[]) docModel.getProperty("dublincore", "contributors");
        UserManager userManager = Framework.getService(UserManager.class);
        return userManager.getPrincipal(contributors[0]);
    }

    protected void setFolderPermissions(CoreSession session, DocumentModel documentModel) {
        ACP acp = documentModel.getACP();
        acp.blockInheritance(ACL.LOCAL_ACL, SecurityConstants.SYSTEM_USERNAME);
        documentModel.setACP(acp, true);
    }

    /**
     * @deprecated since 10.10-HF12. Not used anymore
     */
    @Deprecated
    protected void setCommentPermissions(CoreSession session, DocumentModel documentModel) {
        ACP acp = new ACPImpl();
        ACE grantRead = new ACE(SecurityConstants.EVERYONE, SecurityConstants.READ, true);
        ACE grantRemove = new ACE("members", SecurityConstants.REMOVE, true);
        ACL acl = new ACLImpl();
        acl.setACEs(new ACE[] { grantRead, grantRemove });
        acp.addACL(acl);
        session.setACP(documentModel.getRef(), acp, true);
    }

    protected Collection<String> computeAncestorIds(CoreSession session, String parentId) {
        Collection<String> ancestorIds = new HashSet<>();
        ancestorIds.add(parentId);
        DocumentRef parentRef = new IdRef(parentId);
        while (session.exists(parentRef) && session.getDocument(parentRef).hasSchema(COMMENT_SCHEMA)) {
            parentId = (String) session.getDocument(parentRef).getPropertyValue(COMMENT_PARENT_ID);
            ancestorIds.add(parentId);
            parentRef = new IdRef(parentId);
        }
        return ancestorIds;
    }

    /**
     * Notifies the event of type {@code eventType} on the given {@code commentDocumentModel}.
     *
     * @param session the session
     * @param eventType the event type to fire
     * @param commentDocumentModel the document model of the comment
     * @implSpec This method uses internally {@link #notifyEvent(CoreSession, String, DocumentModel, DocumentModel)}
     * @since 11.1
     */
    protected void notifyEvent(CoreSession session, String eventType, DocumentModel commentDocumentModel) {
        requireNonNull(eventType);

        DocumentModel commentParent = session.getDocument(getCommentedDocumentRef(session, commentDocumentModel));
        notifyEvent(session, eventType, commentParent, commentDocumentModel);
    }

}
