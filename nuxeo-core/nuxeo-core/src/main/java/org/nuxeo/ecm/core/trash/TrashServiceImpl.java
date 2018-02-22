/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.trash;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

public class TrashServiceImpl extends DefaultComponent implements TrashService {

    private static final Log log = LogFactory.getLog(TrashServiceImpl.class);

    public static final String TRASHED_QUERY = "SELECT * FROM Document WHERE ecm:mixinType != 'HiddenInNavigation' AND ecm:isVersion = 0 AND ecm:currentLifeCycleState = 'deleted' AND ecm:parentId = '%s'";

    @Override
    public boolean folderAllowsDelete(DocumentModel folder) {
        return folder.getCoreSession().hasPermission(folder.getRef(), SecurityConstants.REMOVE_CHILDREN);
    }

    @Override
    public boolean checkDeletePermOnParents(List<DocumentModel> docs) {
        if (docs.isEmpty()) {
            return false;
        }
        CoreSession session = docs.get(0).getCoreSession();
        for (DocumentModel doc : docs) {
            if (session.hasPermission(doc.getParentRef(), SecurityConstants.REMOVE_CHILDREN)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canDelete(List<DocumentModel> docs, Principal principal, boolean checkProxies)
            {
        if (docs.isEmpty()) {
            return false;
        }
        // used to do only check on parent perm
        TrashInfo info = getInfo(docs, principal, checkProxies, false);
        return info.docs.size() > 0;
    }

    @Override
    public boolean canPurgeOrUndelete(List<DocumentModel> docs, Principal principal) {
        if (docs.isEmpty()) {
            return false;
        }
        // used to do only check on parent perm
        TrashInfo info = getInfo(docs, principal, false, true);
        return info.docs.size() == docs.size();
    }

    public boolean canUndelete(List<DocumentModel> docs) {
        if (docs.isEmpty()) {
            return false;
        }
        // used to do only check on parent perm
        TrashInfo info = getInfo(docs, null, false, true);
        return info.docs.size() > 0;
    }

    protected TrashInfo getInfo(List<DocumentModel> docs, Principal principal, boolean checkProxies,
            boolean checkDeleted) {
        TrashInfo info = new TrashInfo();
        info.docs = new ArrayList<DocumentModel>(docs.size());
        if (docs.isEmpty()) {
            return info;
        }
        CoreSession session = docs.get(0).getCoreSession();
        for (DocumentModel doc : docs) {
            if (checkDeleted && !LifeCycleConstants.DELETED_STATE.equals(doc.getCurrentLifeCycleState())) {
                info.forbidden++;
                continue;
            }
            if (doc.getParentRef() == null) {
                if (doc.isVersion() && !session.getProxies(doc.getRef(), null).isEmpty()) {
                    // do not remove versions used by proxies
                    info.forbidden++;
                    continue;
                }

            } else {
                if (!session.hasPermission(doc.getParentRef(), SecurityConstants.REMOVE_CHILDREN)) {
                    info.forbidden++;
                    continue;
                }
            }
            if (!session.hasPermission(doc.getRef(), SecurityConstants.REMOVE)) {
                info.forbidden++;
                continue;
            }
            if (checkProxies && doc.isProxy()) {
                info.proxies++;
                continue;
            }
            if (doc.isLocked()) {
                String locker = getDocumentLocker(doc);
                if (principal == null
                        || (principal instanceof NuxeoPrincipal && ((NuxeoPrincipal) principal).isAdministrator())
                        || principal.getName().equals(locker)) {
                    info.docs.add(doc);
                } else {
                    info.locked++;
                }
            } else {
                info.docs.add(doc);
            }
        }
        return info;
    }

    protected static String getDocumentLocker(DocumentModel doc) {
        Lock lock = doc.getLockInfo();
        return lock == null ? null : lock.getOwner();
    }

    /**
     * Path-based comparator used to put folders before their children.
     */
    protected static class PathComparator implements Comparator<DocumentModel>, Serializable {

        private static final long serialVersionUID = 1L;

        public static PathComparator INSTANCE = new PathComparator();

        @Override
        public int compare(DocumentModel doc1, DocumentModel doc2) {
            return doc1.getPathAsString().replace("/", "\u0000").compareTo(
                    doc2.getPathAsString().replace("/", "\u0000"));
        }

    }

    @Override
    public TrashInfo getTrashInfo(List<DocumentModel> docs, Principal principal, boolean checkProxies,
            boolean checkDeleted) {
        TrashInfo info = getInfo(docs, principal, checkProxies, checkDeleted);
        // Keep only common tree roots (see NXP-1411)
        // This is not strictly necessary with Nuxeo Core >= 1.3.2
        Collections.sort(info.docs, PathComparator.INSTANCE);
        List<DocumentModel> roots = new LinkedList<DocumentModel>();
        info.rootPaths = new HashSet<Path>();
        info.rootRefs = new LinkedList<DocumentRef>();
        info.rootParentRefs = new HashSet<DocumentRef>();
        Path previousPath = null;
        for (DocumentModel doc : info.docs) {
            if (previousPath == null || !previousPath.isPrefixOf(doc.getPath())) {
                roots.add(doc);
                Path path = doc.getPath();
                info.rootPaths.add(path);
                info.rootRefs.add(doc.getRef());
                if (doc.getParentRef() != null) {
                    info.rootParentRefs.add(doc.getParentRef());
                }
                previousPath = path;
            }
        }
        return info;
    }

    @Override
    public DocumentModel getAboveDocument(DocumentModel doc, Set<Path> rootPaths) {
        CoreSession session = doc.getCoreSession();
        while (underOneOf(doc.getPath(), rootPaths)) {
            doc = session.getParentDocument(doc.getRef());
            if (doc == null) {
                // handle placeless document
                break;
            }
        }
        return doc;
    }

    protected static boolean underOneOf(Path testedPath, Set<Path> paths) {
        for (Path path : paths) {
            if (path != null && path.isPrefixOf(testedPath)) {
                return true;
            }
        }
        return false;
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
            } else if (session.getCurrentLifeCycleState(docRef).equals(LifeCycleConstants.DELETED_STATE)) {
                log.warn("Document " + doc.getId() + " of type " + doc.getType() + " in state "
                        + doc.getCurrentLifeCycleState() + " is already in state "
                        + LifeCycleConstants.DELETED_STATE + ", nothing to do");
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

    @Override
    public void purgeDocuments(CoreSession session, List<DocumentRef> docRefs) {
        if (docRefs.isEmpty()) {
            return;
        }
        session.removeDocuments(docRefs.toArray(new DocumentRef[docRefs.size()]));
        session.save();
    }

    @Override
    public Set<DocumentRef> undeleteDocuments(List<DocumentModel> docs) {
        Set<DocumentRef> undeleted = new HashSet<DocumentRef>();
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
        Set<DocumentRef> parentRefs = new HashSet<DocumentRef>();
        for (DocumentRef docRef : undeleted) {
            parentRefs.add(session.getParentDocumentRef(docRef));
        }
        // launch async action on folderish to undelete all children recursively
        for (DocumentModel doc : docs) {
            if (doc.isFolder()) {
                notifyEvent(session, LifeCycleConstants.DOCUMENT_UNDELETED, doc);
            }
        }
        return parentRefs;
    }

    protected void notifyEvent(CoreSession session, String eventId, DocumentModel doc) {
        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), doc);
        ctx.setCategory(DocumentEventCategories.EVENT_DOCUMENT_CATEGORY);
        ctx.setProperty(CoreEventConstants.REPOSITORY_NAME, session.getRepositoryName());
        ctx.setProperty(CoreEventConstants.SESSION_ID, session.getSessionId());
        Event event = ctx.newEvent(eventId);
        event.setInline(false);
        event.setImmediate(true);
        EventService eventService = Framework.getService(EventService.class);
        eventService.fireEvent(event);
    }

    /**
     * Undeletes a list of documents. Session is not saved. Log about non-deletable documents.
     */
    protected Set<DocumentRef> undeleteDocumentList(CoreSession session, List<DocumentModel> docs)
            {
        Set<DocumentRef> undeleted = new HashSet<DocumentRef>();
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
    protected void undeleteAncestors(CoreSession session, DocumentRef docRef, Set<DocumentRef> undeleted)
            {
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

    /**
     * Matches names of documents in the trash, created by {@link #trashDocument}.
     */
    protected static final Pattern TRASHED_PATTERN = Pattern.compile("(.*)\\._[0-9]{13,}_\\.trashed");

    /**
     * Matches names resulting from a collision, suffixed with a time in milliseconds, created by DuplicatedNameFixer.
     * We also attempt to remove this when getting a doc out of the trash.
     */
    protected static final Pattern COLLISION_PATTERN = Pattern.compile("(.*)\\.[0-9]{13,}");

    @Override
    public String mangleName(DocumentModel doc) {
        return doc.getName() + "._" + System.currentTimeMillis() + "_.trashed";
    }

    @Override
    public String unmangleName(DocumentModel doc) {
        String name = doc.getName();
        Matcher matcher = TRASHED_PATTERN.matcher(name);
        if (matcher.matches() && matcher.group(1).length() > 0) {
            name = matcher.group(1);
            matcher = COLLISION_PATTERN.matcher(name);
            if (matcher.matches() && matcher.group(1).length() > 0) {
                @SuppressWarnings("resource")
                CoreSession session = doc.getCoreSession();
                if (session != null) {
                    String orig = matcher.group(1);
                    String parentPath = session.getDocument(doc.getParentRef()).getPathAsString();
                    if (parentPath.equals("/")) {
                        parentPath = ""; // root
                    }
                    String newPath = parentPath + "/" + orig;
                    if (!session.exists(new PathRef(newPath))) {
                        name = orig;
                    }
                }
            }
        }
        return name;
    }

    protected void trashDocument(CoreSession session, DocumentModel doc) {
        String name = mangleName(doc);
        if (doc.getParentRef() == null) {
            // handle placeless document
            session.removeDocument(doc.getRef());
        } else {
            session.move(doc.getRef(), doc.getParentRef(), name);
            session.followTransition(doc, LifeCycleConstants.DELETE_TRANSITION);
        }
    }

    protected void undeleteDocument(CoreSession session, DocumentModel doc) {
        String name = doc.getName();
        String newName = unmangleName(doc);
        if (!newName.equals(name)) {
            session.move(doc.getRef(), doc.getParentRef(), newName);
        }
        session.followTransition(doc, LifeCycleConstants.UNDELETE_TRANSITION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DocumentModelList getDocuments(DocumentModel currentDoc) {
        CoreSession session = currentDoc.getCoreSession();
        DocumentModelList docs = session.query(String.format(
                TRASHED_QUERY,
                currentDoc.getId()));
        return docs;
    }

    @Override
    public List<DocumentRef> getPurgeableDocumentRefs(DocumentModel parent) {
        List<DocumentRef> refs = new ArrayList<>();
        CoreSession session = parent.getCoreSession();
        if (!session.hasPermission(parent.getParentRef(), SecurityConstants.REMOVE_CHILDREN)) {
            return refs;
        }
        try (IterableQueryResult result = session.queryAndFetch(String.format(TRASHED_QUERY, parent.getId()),
                NXQL.NXQL)) {
            for (Map<String, Serializable> map : result) {
                DocumentRef ref = new IdRef((String) map.get(NXQL.ECM_UUID));
                if (!session.hasPermission(ref, SecurityConstants.REMOVE)) {
                    continue;
                }
                DocumentModel doc = session.getDocument(ref);
                if (doc.isLocked()) {
                    String locker = getDocumentLocker(doc);
                    NuxeoPrincipal principal = (NuxeoPrincipal) session.getPrincipal();
                    if (principal != null && !((NuxeoPrincipal) principal).isAdministrator()
                            && !principal.getName().equals(locker)) {
                        continue;
                    }
                }
                refs.add(new IdRef((String) map.get(NXQL.ECM_UUID)));
            }
        }
        return refs;
    }

}
