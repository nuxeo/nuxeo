/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.trash;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.security.SecurityConstants;

/**
 * @deprecated since 10.1, use {@link PropertyTrashService} instead.
 */
@Deprecated
public class LifeCycleTrashService extends AbstractTrashService {

    private static final Log log = LogFactory.getLog(LifeCycleTrashService.class);

    @Override
    public boolean isTrashed(CoreSession session, DocumentRef docRef) {
        return LifeCycleConstants.DELETED_STATE.equals(session.getCurrentLifeCycleState(docRef));
    }

    @Override
    public void trashDocuments(List<DocumentModel> docs) {
        if (docs.isEmpty()) {
            return;
        }
        CoreSession session = docs.get(0).getCoreSession();
        for (DocumentModel doc : docs) {
            DocumentRef docRef = doc.getRef();
            if (session.getAllowedStateTransitions(docRef).contains(LifeCycleConstants.DELETE_TRANSITION)
                    && !doc.isProxy()) {
                if (!session.canRemoveDocument(docRef)) {
                    throw new DocumentSecurityException("User " + session.getPrincipal().getName()
                            + " does not have the permission to remove the document " + doc.getId() + " ("
                            + doc.getPath() + ")");
                }
                trashDocument(session, doc);
            } else if (session.isTrashed(docRef)) {
                log.warn("Document " + doc.getId() + " of type " + doc.getType()
                        + " is already in the trash, nothing to do");
                return;
            } else {
                log.warn("Document " + doc.getId() + " of type " + doc.getType() + " in state "
                        + doc.getCurrentLifeCycleState() + " does not support transition "
                        + LifeCycleConstants.DELETE_TRANSITION + ", it will be deleted immediately");
                session.removeDocument(docRef);
            }
        }
        session.save();
    }

    protected void trashDocument(CoreSession session, DocumentModel doc) {
        if (doc.getParentRef() == null) {
            // handle placeless document
            session.removeDocument(doc.getRef());
        } else {
            if (!Boolean.TRUE.equals(doc.getContextData(DISABLE_TRASH_RENAMING))) {
                String name = mangleName(doc);
                session.move(doc.getRef(), doc.getParentRef(), name);
            }
            session.followTransition(doc, LifeCycleConstants.DELETE_TRANSITION);
        }
    }

    @Override
    public Set<DocumentRef> undeleteDocuments(List<DocumentModel> docs) {
        Set<DocumentRef> undeleted = new HashSet<>();
        if (docs.isEmpty()) {
            return undeleted;
        }
        CoreSession session = docs.get(0).getCoreSession();
        Set<DocumentRef> docRefs = undeleteDocumentList(session, docs);
        undeleted.addAll(docRefs);
        // undeleted ancestors
        for (DocumentRef docRef : docRefs) {
            undeleteAncestors(session, docRef, undeleted);
        }
        session.save();
        // find parents of undeleted docs (for notification);
        Set<DocumentRef> parentRefs = new HashSet<>();
        for (DocumentRef docRef : undeleted) {
            parentRefs.add(session.getParentDocumentRef(docRef));
        }
        // launch async action on folderish to undelete all children recursively
        for (DocumentModel doc : docs) {
            if (doc.isFolder()) {
                notifyEvent(session, LifeCycleConstants.DOCUMENT_UNDELETED, doc, true);
            }
        }
        return parentRefs;
    }

    /**
     * Undeletes a list of documents. Session is not saved. Log about non-deletable documents.
     */
    protected Set<DocumentRef> undeleteDocumentList(CoreSession session, List<DocumentModel> docs) {
        Set<DocumentRef> undeleted = new HashSet<>();
        for (DocumentModel doc : docs) {
            DocumentRef docRef = doc.getRef();
            if (session.getAllowedStateTransitions(docRef).contains(LifeCycleConstants.UNDELETE_TRANSITION)) {
                undeleteDocument(session, doc);
                undeleted.add(docRef);
            } else {
                log.debug("Impossible to undelete document " + docRef + " as it does not support transition "
                        + LifeCycleConstants.UNDELETE_TRANSITION);
            }
        }
        return undeleted;
    }

    /**
     * Undeletes ancestors of a document. Session is not saved. Stops as soon as an ancestor is not undeletable.
     */
    protected void undeleteAncestors(CoreSession session, DocumentRef docRef, Set<DocumentRef> undeleted) {
        for (DocumentRef ancestorRef : session.getParentDocumentRefs(docRef)) {
            // getting allowed state transitions and following a transition need
            // ReadLifeCycle and WriteLifeCycle
            if (session.hasPermission(ancestorRef, SecurityConstants.READ_LIFE_CYCLE)
                    && session.hasPermission(ancestorRef, SecurityConstants.WRITE_LIFE_CYCLE)) {
                if (session.getAllowedStateTransitions(ancestorRef).contains(LifeCycleConstants.UNDELETE_TRANSITION)) {
                    DocumentModel ancestor = session.getDocument(ancestorRef);
                    undeleteDocument(session, ancestor);
                    undeleted.add(ancestorRef);
                } else {
                    break;
                }
            } else {
                // stop if lifecycle properties can't be read on an ancestor
                log.debug("Stopping to restore ancestors because " + ancestorRef.toString() + " is not readable");
                break;
            }
        }
    }

    protected void undeleteDocument(CoreSession session, DocumentModel doc) {
        String name = doc.getName();
        if (!Boolean.TRUE.equals(doc.getContextData(DISABLE_TRASH_RENAMING))) {
            String newName = unmangleName(doc);
            if (!newName.equals(name)) {
                session.move(doc.getRef(), doc.getParentRef(), newName);
            }
        }
        session.followTransition(doc, LifeCycleConstants.UNDELETE_TRANSITION);
    }

    @Override
    public boolean hasFeature(Feature feature) {
        switch (feature) {
            case TRASHED_STATE_IS_DEDUCED_FROM_LIFECYCLE:
                return true;
            case TRASHED_STATE_IN_MIGRATION:
            case TRASHED_STATE_IS_DEDICATED_PROPERTY:
                return false;
        default:
            throw new UnsupportedOperationException(feature.name());
        }
    }

}
