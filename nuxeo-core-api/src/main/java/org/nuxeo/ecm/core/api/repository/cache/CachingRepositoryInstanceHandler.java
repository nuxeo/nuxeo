/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.repository.cache;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelIterator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryExceptionHandler;
import org.nuxeo.ecm.core.api.repository.RepositoryInstance;
import org.nuxeo.ecm.core.api.repository.RepositoryInstanceHandler;

/**
 *
 * Cached children are not preserving order
 * The order should be updated from notifications
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class CachingRepositoryInstanceHandler extends RepositoryInstanceHandler
        implements DocumentModelCache {

    protected Principal principal;
    protected String sessionId;

    protected final ConcurrentMap<String, DocumentModel> cache = new ConcurrentHashMap<String, DocumentModel>();
    protected final ConcurrentMap<String, String> path2Ids = new ConcurrentHashMap<String, String>();

    /** Children Cache: parentId -> list of child Ids  */
    protected final Map<String, List<DocumentRef>> childrenCache = new HashMap<String, List<DocumentRef>>();

    // TODO fix sync pb


    public CachingRepositoryInstanceHandler(Repository repository) {
        super(repository);
    }

    public CachingRepositoryInstanceHandler(Repository repository,
            RepositoryExceptionHandler exceptionHandler) {
        super(repository, exceptionHandler);
    }

    @Override
    public Class<?>[] getProxyInterfaces() {
        return new Class[]{RepositoryInstance.class, DocumentModelCache.class};
    }

    public Principal getPrincipal() throws Exception {
        if (principal == null) {
            principal = getSession().getPrincipal();
        }
        return principal;
    }

    public String getSessionId() throws Exception {
        if (sessionId == null) {
            sessionId = getSession().getSessionId();
        }
        return sessionId;
    }

    public String getRepositoryName() {
        return repository.getName();
    }


    /**
     * --------------------------- Document Provider API -------------------------------- The doc
     * cache should be allways updated first (before paths cache). It is not a blocking issue if we
     * end up with garbage in the path cache (path mappings to IDs that doesn't exists anymore in
     * the doc cache)
     */

    public DocumentModel cacheDocument(DocumentModel doc) {
        String id = doc.getId();
        if (id != null) {
            DocumentModel cachedDoc = cache.putIfAbsent(id, doc);
            if (cachedDoc == null) { // here we may end up with id and paths being unsync - but it is not a pb.
                path2Ids.put(doc.getPathAsString(), id);
                return doc;
            }
            return cachedDoc;
        } // else doc is not yet stored in repository - avoid caching it
        return doc;
    }

    public DocumentModel uncacheDocument(DocumentRef ref) {
        if (ref.type() == DocumentRef.ID) {
            String id = ((IdRef) ref).value;
            DocumentModel doc = cache.remove(id);
            if (doc != null) {
                path2Ids.remove(doc.getPathAsString());
            }
            return doc;
        }
        // else assume a path
        String path = ((PathRef) ref).value;
        String id = path2Ids.remove(path);
        if (id != null) {
            return cache.remove(id);
        }
        return null;
    }

    public DocumentModel getCachedDocument(DocumentRef ref) {
        if (ref.type() == DocumentRef.ID) {
            return cache.get(((IdRef) ref).value);
        } // else assume a path
        String id = path2Ids.get(((PathRef) ref).value);
        if (id != null) {
            return cache.get(id);
        }
        return null;
    }

    public void flushDocumentCache() {
        // Race condition: try to clean until we succeed - this may not work from first time
        // because we are not in a synchronized block
        while (path2Ids.isEmpty() && cache.isEmpty()) {
            path2Ids.clear();
            cache.clear();
        }
    }

    public DocumentModel fetchDocument(DocumentRef ref) throws ClientException {
        DocumentModel doc = getCachedDocument(ref);
        if (doc != null) {
            doc.refresh(DocumentModel.REFRESH_ALL, null);
            return doc;
        }
        return cacheDocument(session.getDocument(ref));
    }

    public DocumentModel getChild(DocumentRef parent, String name) throws ClientException {
        DocumentModel doc = getCachedDocument(parent);
        if (doc != null) {
            String path = doc.getPathAsString();
            path = new StringBuffer(path.length() + 256).append(path).append("/").append(
                    name).toString();
            String id = path2Ids.get(path);
            if (id != null) {
                doc = cache.get(id);
                if (doc != null) {// the two maps may become unsynchrnized after a delete
                    return doc;
                }
            }
        } // document not in cache
        return cacheDocument(session.getChild(parent, name));
    }

    public DocumentModel getRootDocument() throws ClientException {
        String id = path2Ids.get("/");
        if (id != null) {
            DocumentModel doc = cache.get(id);
            if (doc != null) {
                return doc;
            }
        } // cannot find the root doc in cache
        return cacheDocument(session.getRootDocument());
    }


    public DocumentModel getDocument(DocumentRef ref) throws ClientException {
        DocumentModel doc = getCachedDocument(ref);
        return doc != null ? doc
                : cacheDocument(session.getDocument(ref));
    }

    public DocumentModel getParentDocument(DocumentRef ref) throws ClientException {
        DocumentModel doc = getCachedDocument(ref);
        if (doc != null) {
            return getDocument(doc.getParentRef());
        }
        return cacheDocument(session.getParentDocument(ref));
    }

    public DocumentModelList getChildren(DocumentRef parent) throws ClientException {
        String id = getDocumentId(parent);
        if (id != null) {
            DocumentModelList result = getCachedChildren(parent);
            return result != null ? result
                    : fetchAndCacheChildren(parent);
        }
        return new DocumentModelListImpl(); // empty children
    }

    public DocumentModelIterator getChildrenIterator(DocumentRef parent) throws ClientException {
        return new SimpleDocumentModelIterator(getChildren(parent));
    }

    public DocumentModelList query(String query) throws ClientException {
        return new CachingDocumentList(this, session.query(query));
    }

    public DocumentModelList getFiles(DocumentRef parent) throws ClientException {
        // get all children and filter locally
        DocumentModelList docs = getCachedChildrenWithoutFacet(parent, "Folderish");
        if (docs == null) {
            docs = filterWithoutFacet(fetchAndCacheChildren(parent), "Folderish");
        }
        return docs;
    }

    public DocumentModelList getFolders(DocumentRef parent) throws ClientException {
        // get all children and filter locally
        DocumentModelList docs = getCachedChildrenWithFacet(parent, "Folderish");
        if (docs == null) {
            docs = filterByFacet(fetchAndCacheChildren(parent), "Folderish");
        }
        return docs;
    }

    public DocumentModelList getChildren(DocumentRef parent, String type) throws ClientException {
        // get all children and filter locally
        DocumentModelList docs = getCachedChildrenWithType(parent, type);
        if (docs == null) {
            docs = filterByType(fetchAndCacheChildren(parent), type);
        }
        return docs;
    }

    public DocumentModel createDocument(DocumentModel doc) throws ClientException {
        return cacheDocument(session.createDocument(doc));
    }

    public DocumentModel[] createDocument(DocumentModel[] docs) throws ClientException {
        docs = session.createDocument(docs);
        for (int i = docs.length - 1; i >= 0; i--) {
            docs[i] = cacheDocument(docs[i]);
        }
        return docs;
    }

    public DocumentModel createDocumentModel(String type) throws ClientException {
        return cacheDocument(session.createDocumentModel(type));
    }

    public DocumentModel createDocumentModel(String type, Map<String, Object> options)
            throws ClientException {
        return cacheDocument(session.createDocumentModel(type, options));
    }

    public DocumentModel createDocumentModel(String parentPath, String id, String type)
            throws ClientException {
        return cacheDocument(session.createDocumentModel(parentPath, id, type));
    }

    public DocumentModel createProxy(DocumentRef parentRef, DocumentRef docRef,
            VersionModel version, boolean overwriteExistingProxy) throws ClientException {
        return cacheDocument(
                session.createProxy(parentRef, docRef, version, overwriteExistingProxy));
    }


    /** Children Cache */

    public String getDocumentId(DocumentRef docRef) {
        switch (docRef.type()) {
        case DocumentRef.ID:
            return (String) docRef.reference();
        case DocumentRef.PATH:
            String path = (String)docRef.reference();
            return path2Ids.get(path);
        default:
            return null;
        }
    }

    /**
     * This will modify the given list and replace documents with the cached versions.
     */
    public void cacheChildren(DocumentRef parent, DocumentModelList children) throws ClientException {
        String id = getDocumentId(parent);
        if (id != null) {
            List<DocumentRef> cache = new ArrayList<DocumentRef>();
            for (int i=0, len=children.size(); i<len; i++) {
                DocumentModel child = children.get(i);
                child = cacheDocument(child);
                children.set(i, child); // replace by the cached document
                cache.add(child.getRef());
            }
            synchronized (childrenCache) {
                childrenCache.put(id, cache);
            }
        }
    }

    public void uncacheChildren(DocumentRef parent) {
        String id = getDocumentId(parent);
        if (id != null) {
            synchronized (childrenCache) {
                childrenCache.remove(id);
            }
        }
    }

    public DocumentModelList fetchChildren(DocumentRef parent)
            throws Exception {
        return getSession().getChildren(parent);
    }

    public DocumentModelList  filterByFacet(DocumentModelList docs, String facet) {
        DocumentModelList result = new DocumentModelListImpl();
        for (DocumentModel doc : docs) {
            if (doc.hasFacet(facet)) {
                result.add(doc);
            }
        }
        return result;
    }

    public DocumentModelList  filterWithoutFacet(DocumentModelList docs, String facet) {
        DocumentModelList result = new DocumentModelListImpl();
        for (DocumentModel doc : docs) {
            if (!doc.hasFacet(facet)) {
                result.add(doc);
            }
        }
        return result;
    }

    public DocumentModelList  filterByType(DocumentModelList docs, String type) {
        DocumentModelList result = new DocumentModelListImpl();
        for (DocumentModel doc : docs) {
            if (type.equals(doc.getType())) {
                result.add(doc);
            }
        }
        return result;
    }

    public DocumentModelList fetchAndCacheChildren(DocumentRef parent) throws ClientException {
        try {
            DocumentModelList children =  getSession().getChildren(parent);
            cacheChildren(parent, children);
            return children;
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            throw new ClientException("Failed to get proxy session", e);
        }
    }

    public DocumentModelList getCachedChildren(DocumentRef parent) throws ClientException {
        String id = getDocumentId(parent);
        if (id != null) {
            List<DocumentRef> children = null;
            synchronized (childrenCache) {
                children = childrenCache.get(id);
            }
            if (children != null) {
                // avoid concurrent modifications by using a copy (an array)
                DocumentRef[] refs = children.toArray(new DocumentRef[children.size()]);
                DocumentModelList result = new DocumentModelListImpl();
                for (DocumentRef ref : refs) {
                    result.add(getDocument(ref));
                }
                return result;
            }
        }
        return null;
    }

    public DocumentModelList getCachedChildrenWithType(DocumentRef parent, String type) throws ClientException {
        String id = getDocumentId(parent);
        if (id != null) {
            List<DocumentRef> children = null;
            synchronized (childrenCache) {
                children = childrenCache.get(id);
            }
            if (children != null) {
                // avoid concurrent modifications by using a copy (an array)
                DocumentRef[] refs = children.toArray(new DocumentRef[children.size()]);
                DocumentModelList result = new DocumentModelListImpl();
                for (DocumentRef ref : refs) {
                    DocumentModel doc = getDocument(ref);
                    if (type.equals(doc.getType())) {
                        result.add(doc);
                    }
                }
                return result;
            }
        }
        return null;
    }

    public DocumentModelList getCachedChildrenWithFacet(DocumentRef parent, String facet) throws ClientException {
        String id = getDocumentId(parent);
        if (id != null) {
            List<DocumentRef> children = null;
            synchronized (childrenCache) {
                children = childrenCache.get(id);
            }
            if (children != null) {
                // avoid concurrent modifications by using a copy (an array)
                DocumentRef[] refs = children.toArray(new DocumentRef[children.size()]);
                DocumentModelList result = new DocumentModelListImpl();
                for (DocumentRef ref : refs) {
                    DocumentModel doc = getDocument(ref);
                    if (doc.hasFacet(facet)) {
                        result.add(doc);
                    }
                }
                return result;
            }
        }
        return null;
    }

    public DocumentModelList getCachedChildrenWithoutFacet(DocumentRef parent, String facet) throws ClientException {
        String id = getDocumentId(parent);
        if (id != null) {
            List<DocumentRef> children = null;
            synchronized (childrenCache) {
                children = childrenCache.get(id);
            }
            if (children != null) {
                // avoid concurrent modifications by using a copy (an array)
                DocumentRef[] refs = children.toArray(new DocumentRef[children.size()]);
                DocumentModelList result = new DocumentModelListImpl();
                for (DocumentRef ref : refs) {
                    DocumentModel doc = getDocument(ref);
                    if (!doc.hasFacet(facet)) {
                        result.add(doc);
                    }
                }
                return result;
            }
        }
        return null;
    }

    public void cacheChild(DocumentRef parent, DocumentRef child) {
        String id = getDocumentId(parent);
        if (id != null) {
            synchronized (childrenCache) {
                List<DocumentRef> list = childrenCache.get(id);
                if (list == null) {
                    list = new ArrayList<DocumentRef>();
                    childrenCache.put(id, list);
                }
                list.add(child);
            }
        }
    }

    public void uncacheChild(DocumentRef parent, DocumentRef child) {
        String id = getDocumentId(parent);
        if (id != null) {
            synchronized (childrenCache) {
                List<DocumentRef> list = childrenCache.get(id);
                if (list != null) {
                    list.remove(child);
                }
            }
        }
    }

}
