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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
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
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.runtime.api.Framework;

/**
 * Basic implementation of {@link TrashService}.
 *
 * @since 10.1
 */
public abstract class AbstractTrashService implements TrashService {

    public static final String TRASHED_QUERY = "SELECT * FROM Document WHERE ecm:mixinType != 'HiddenInNavigation' AND ecm:isVersion = 0 AND ecm:isTrashed = 1 AND ecm:parentId = '%s'";

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
    public boolean canDelete(List<DocumentModel> docs, NuxeoPrincipal principal, boolean checkProxies) {
        if (docs.isEmpty()) {
            return false;
        }
        // used to do only check on parent perm
        TrashInfo info = getInfo(docs, principal, checkProxies, false);
        return info.docs.size() > 0;
    }

    @Override
    public boolean canPurgeOrUntrash(List<DocumentModel> docs, NuxeoPrincipal principal) {
        if (docs.isEmpty()) {
            return false;
        }
        // used to do only check on parent perm
        TrashInfo info = getInfo(docs, principal, false, true);
        return info.docs.size() == docs.size();
    }

    protected TrashInfo getInfo(List<DocumentModel> docs, NuxeoPrincipal principal, boolean checkProxies,
            boolean checkDeleted) {
        TrashInfo info = new TrashInfo();
        info.docs = new ArrayList<>(docs.size());
        if (docs.isEmpty()) {
            return info;
        }
        CoreSession session = docs.get(0).getCoreSession();
        for (DocumentModel doc : docs) {
            if (checkDeleted && !doc.isTrashed()) {
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
                if (principal == null || principal.isAdministrator() || principal.getName().equals(locker)) {
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

        public static final PathComparator INSTANCE = new PathComparator();

        @Override
        public int compare(DocumentModel doc1, DocumentModel doc2) {
            return doc1.getPathAsString().replace("/", "\u0000").compareTo(
                    doc2.getPathAsString().replace("/", "\u0000"));
        }

    }

    @Override
    public TrashInfo getTrashInfo(List<DocumentModel> docs, NuxeoPrincipal principal, boolean checkProxies,
            boolean checkDeleted) {
        TrashInfo info = getInfo(docs, principal, checkProxies, checkDeleted);
        // Keep only common tree roots (see NXP-1411)
        // This is not strictly necessary with Nuxeo Core >= 1.3.2
        info.docs.sort(PathComparator.INSTANCE);
        info.rootPaths = new HashSet<>();
        info.rootRefs = new LinkedList<>();
        info.rootParentRefs = new HashSet<>();
        Path previousPath = null;
        for (DocumentModel doc : info.docs) {
            if (previousPath == null || !previousPath.isPrefixOf(doc.getPath())) {
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

    @Override
    public DocumentModel getAboveDocument(DocumentModel doc, NuxeoPrincipal principal) {
        TrashInfo info = getTrashInfo(Collections.singletonList(doc), principal, false, false);
        return getAboveDocument(doc, info.rootPaths);
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
    public void purgeDocuments(CoreSession session, List<DocumentRef> docRefs) {
        if (docRefs.isEmpty()) {
            return;
        }
        session.removeDocuments(docRefs.toArray(new DocumentRef[docRefs.size()]));
        session.save();
    }

    @Override
    public void purgeDocumentsUnder(DocumentModel parent) {
        if (parent == null || !parent.hasFacet(FacetNames.FOLDERISH)) {
            throw new UnsupportedOperationException("Empty trash can only be performed on a Folderish document");
        }
        CoreSession session = parent.getCoreSession();
        if (!session.hasPermission(parent.getParentRef(), SecurityConstants.REMOVE_CHILDREN)) {
            return;
        }
        try (IterableQueryResult result = session.queryAndFetch(String.format(TRASHED_QUERY, parent.getId()),
                NXQL.NXQL)) {
            NuxeoPrincipal principal = session.getPrincipal();
            StreamSupport.stream(result.spliterator(), false)
                         .map(map -> map.get(NXQL.ECM_UUID).toString())
                         .map(IdRef::new)
                         // check user has permission to remove document
                         .filter(ref -> session.hasPermission(ref, SecurityConstants.REMOVE))
                         // check user has permission to remove a locked document
                         .filter(ref -> {
                             if (principal == null || principal.isAdministrator()) {
                                 // administrator can remove anything
                                 return true;
                             } else {
                                 // only lock owner can remove locked document
                                 DocumentModel doc = session.getDocument(ref);
                                 return !doc.isLocked() || principal.getName().equals(getDocumentLocker(doc));
                             }
                         })
                         .forEach(session::removeDocument);
        }
        session.save();
    }

    protected void notifyEvent(CoreSession session, String eventId, DocumentModel doc) {
        notifyEvent(session, eventId, doc, false);
    }

    protected void notifyEvent(CoreSession session, String eventId, DocumentModel doc, boolean immediate) {
        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), doc);
        ctx.setProperties(new HashMap<>(doc.getContextData()));
        ctx.setCategory(DocumentEventCategories.EVENT_DOCUMENT_CATEGORY);
        ctx.setProperty(CoreEventConstants.REPOSITORY_NAME, session.getRepositoryName());
        ctx.setProperty(CoreEventConstants.SESSION_ID, session.getSessionId());
        Event event = ctx.newEvent(eventId);
        event.setInline(false);
        event.setImmediate(immediate);
        EventService eventService = Framework.getService(EventService.class);
        eventService.fireEvent(event);
    }

    @Override
    public DocumentModelList getDocuments(DocumentModel parent) {
        CoreSession session = parent.getCoreSession();
        return session.query(String.format(TRASHED_QUERY, parent.getId()));
    }

    @Override
    public void untrashDocuments(List<DocumentModel> docs) {
        undeleteDocuments(docs);
    }

    /**
     * Matches names of documents in the trash, created by {@link #trashDocuments(List)}.
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

}
