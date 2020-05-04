/*
 * (C) Copyright 2018-2020 Nuxeo (http://nuxeo.com/) and others.
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
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_AUTHOR_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_PARENT_ID_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_SCHEMA;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_TEXT_PROPERTY;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreInstance;
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
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentNotFoundException;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentSecurityException;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 10.3
 */
public abstract class AbstractCommentManager implements CommentManager {

    private static final Logger log = LogManager.getLogger(AbstractCommentManager.class);

    /** @since 11.1 */
    public static final String COMMENTS_DIRECTORY = "Comments";

    @Override
    public List<DocumentModel> getComments(DocumentModel docModel) {
        return getComments(docModel.getCoreSession(), docModel);
    }

    @Override
    @SuppressWarnings("removal")
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
    public DocumentRef getTopLevelDocumentRef(CoreSession session, DocumentRef commentRef) {
        NuxeoPrincipal principal = session.getPrincipal();
        return CoreInstance.doPrivileged(session, s -> {
            if (!s.exists(commentRef)) {
                throw new CommentNotFoundException(String.format("The comment %s does not exist.", commentRef));
            }

            DocumentModel commentDoc = s.getDocument(commentRef);
            DocumentModel topLevelDoc = getTopLevelDocument(s, commentDoc);
            DocumentRef topLevelDocRef = topLevelDoc.getRef();

            if (!s.hasPermission(principal, topLevelDocRef, SecurityConstants.READ)) {
                throw new CommentSecurityException("The user " + principal.getName()
                        + " does not have access to the comments of document " + topLevelDocRef);
            }

            return topLevelDocRef;
        });
    }

    /**
     * Notifies the event of type {@code eventType} on the given {@code commentDoc}.
     *
     * @param session the session
     * @param eventType the event type to fire
     * @param commentDoc the document model of the comment
     * @implSpec This method uses internally {@link #notifyEvent(CoreSession, String, DocumentModel, DocumentModel)}
     * @since 11.1
     */
    protected void notifyEvent(CoreSession session, String eventType, DocumentModel commentDoc) {
        DocumentModel commentedDoc = getCommentedDocument(session, commentDoc);
        notifyEvent(session, eventType, commentedDoc, commentDoc);
    }

    protected void notifyEvent(CoreSession session, String eventType, DocumentModel commentedDoc,
            DocumentModel commentDoc) {
        DocumentModel topLevelDoc = getTopLevelDocument(session, commentDoc);
        notifyEvent(session, eventType, topLevelDoc, commentedDoc, commentDoc);
    }

    /**
     * @since 11.1
     */
    protected void notifyEvent(CoreSession session, String eventType, DocumentModel topLevelDoc,
            DocumentModel commentedDoc, DocumentModel commentDoc) {
        requireNonNull(eventType);
        UserManager userManager = Framework.getService(UserManager.class);
        NuxeoPrincipal principal = null;
        if (userManager != null) {
            principal = userManager.getPrincipal((String) commentDoc.getPropertyValue(COMMENT_AUTHOR_PROPERTY));
            if (principal == null) {
                try {
                    principal = getAuthor(commentDoc);
                } catch (PropertyException e) {
                    log.error("Error building principal for comment author", e);
                    return;
                }
            }
        }
        DocumentEventContext ctx = new DocumentEventContext(session, principal, commentedDoc);
        Map<String, Serializable> props = new HashMap<>();
        props.put(CommentConstants.TOP_LEVEL_DOCUMENT, topLevelDoc);
        props.put(CommentConstants.PARENT_COMMENT, commentedDoc);
        // simplifies template checks and vars expansion
        if (!topLevelDoc.equals(commentedDoc)) {
            String commentAuthor;
            NuxeoPrincipal commentPrincipal = getAuthor(commentedDoc);
            if (commentPrincipal != null) {
                commentAuthor = commentPrincipal.getFirstName();
                commentAuthor = isBlank(commentAuthor) ? commentPrincipal.getName() : commentAuthor;
            } else {
                commentAuthor = ((String[]) commentedDoc.getPropertyValue("dc:contributors"))[0];
            }
            props.put(CommentConstants.PARENT_COMMENT_AUTHOR, commentAuthor);
        }
        props.put(CommentConstants.COMMENT_DOCUMENT, commentDoc);
        props.put(CommentConstants.COMMENT, commentDoc.getPropertyValue(COMMENT_TEXT_PROPERTY));
        // Keep comment_text for compatibility
        props.put(CommentConstants.COMMENT_TEXT, commentDoc.getPropertyValue(COMMENT_TEXT_PROPERTY));
        props.put("category", CommentConstants.EVENT_COMMENT_CATEGORY);
        ctx.setProperties(props);
        Event event = ctx.newEvent(eventType);

        EventProducer evtProducer = Framework.getService(EventProducer.class);
        evtProducer.fireEvent(event);
    }

    protected abstract DocumentModel getTopLevelDocument(CoreSession session, DocumentModel commentDoc);

    protected abstract DocumentModel getCommentedDocument(CoreSession session, DocumentModel commentDoc);

    protected NuxeoPrincipal getAuthor(DocumentModel docModel) {
        String author = null;
        if (docModel.hasSchema(COMMENT_SCHEMA)) {
            // means annotation / comment
            author = (String) docModel.getPropertyValue(COMMENT_AUTHOR_PROPERTY);
        }
        if (StringUtils.isBlank(author)) {
            String[] contributors = (String[]) docModel.getPropertyValue("dc:contributors");
            author = contributors[0];
        }

        NuxeoPrincipal principal = Framework.getService(UserManager.class).getPrincipal(author);
        // If principal doesn't exist anymore
        if (principal == null) {
            log.debug("Principal not found: {}", author);
        }
        return principal;
    }

    protected void setFolderPermissions(CoreSession session, DocumentModel documentModel) {
        ACP acp = documentModel.getACP();
        acp.blockInheritance(ACL.LOCAL_ACL, SecurityConstants.SYSTEM_USERNAME);
        documentModel.setACP(acp, true);
    }

    /**
     * @deprecated since 11.1. Not used anymore
     */
    @Deprecated(since = "11.1")
    protected void setCommentPermissions(CoreSession session, DocumentModel documentModel) {
        ACP acp = new ACPImpl();
        ACE grantRead = new ACE(SecurityConstants.EVERYONE, SecurityConstants.READ, true);
        ACE grantRemove = new ACE("members", SecurityConstants.REMOVE, true);
        ACL acl = new ACLImpl();
        acl.setACEs(new ACE[] { grantRead, grantRemove });
        acp.addACL(acl);
        session.setACP(documentModel.getRef(), acp, true);
    }

    protected void fillCommentForCreation(CoreSession session, Comment comment) {
        // Initiate Author if it is not done yet
        if (comment.getAuthor() == null) {
            comment.setAuthor(session.getPrincipal().getName());
        }

        // Initiate Creation Date if it is not done yet
        if (comment.getCreationDate() == null) {
            comment.setCreationDate(Instant.now());
        }

        // Initiate Modification Date if it is not done yet
        if (comment.getModificationDate() == null) {
            comment.setModificationDate(Instant.now());
        }
    }

    /**
     * @param session the session allowing to get parent documents, depending on implementation it should be privileged
     */
    @SuppressWarnings("unchecked")
    protected <S extends Set<String> & Serializable> S computeAncestorIds(CoreSession session, String parentId) {
        Set<String> ancestorIds = new HashSet<>();
        ancestorIds.add(parentId);
        DocumentRef parentRef = new IdRef(parentId);
        while (session.exists(parentRef) && session.getDocument(parentRef).hasSchema(COMMENT_SCHEMA)) {
            parentId = (String) session.getDocument(parentRef).getPropertyValue(COMMENT_PARENT_ID_PROPERTY);
            ancestorIds.add(parentId);
            parentRef = new IdRef(parentId);
        }
        return (S) ancestorIds;
    }

}
