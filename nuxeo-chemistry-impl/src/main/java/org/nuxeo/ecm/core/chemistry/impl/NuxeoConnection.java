/*
 * Copyright 2009 Nuxeo SA <http://nuxeo.com>
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
 * Authors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.chemistry.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.apache.chemistry.ACE;
import org.apache.chemistry.ACLPropagation;
import org.apache.chemistry.BaseType;
import org.apache.chemistry.CMISObject;
import org.apache.chemistry.CMISRuntimeException;
import org.apache.chemistry.Connection;
import org.apache.chemistry.ContentStream;
import org.apache.chemistry.Document;
import org.apache.chemistry.Folder;
import org.apache.chemistry.Inclusion;
import org.apache.chemistry.ListPage;
import org.apache.chemistry.ObjectEntry;
import org.apache.chemistry.ObjectId;
import org.apache.chemistry.Paging;
import org.apache.chemistry.Policy;
import org.apache.chemistry.Property;
import org.apache.chemistry.PropertyDefinition;
import org.apache.chemistry.Relationship;
import org.apache.chemistry.Rendition;
import org.apache.chemistry.Repository;
import org.apache.chemistry.SPI;
import org.apache.chemistry.Type;
import org.apache.chemistry.Unfiling;
import org.apache.chemistry.VersioningState;
import org.apache.chemistry.impl.base.BaseRepository;
import org.apache.chemistry.impl.simple.SimpleConnection;
import org.apache.chemistry.impl.simple.SimpleData;
import org.apache.chemistry.impl.simple.SimpleListPage;
import org.apache.chemistry.impl.simple.SimpleObjectEntry;
import org.apache.chemistry.impl.simple.SimpleObjectId;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.security.SecurityPolicyService;
import org.nuxeo.runtime.api.Framework;

public class NuxeoConnection implements Connection, SPI {

    private static final Log log = LogFactory.getLog(NuxeoConnection.class);

    protected final NuxeoRepository repository;

    protected final CoreSession session;

    protected final NuxeoFolder rootFolder;

    /** map of lowercase type name to actual Nuxeo type name */
    protected final Map<String, String> queryTypeNames;

    /** map of Nuxeo and CMIS prop names to NXQL names */
    protected final Map<String, String> queryPropNames;

    public NuxeoConnection(NuxeoRepository repository,
            Map<String, Serializable> parameters) {
        this.repository = repository;

        // TODO map parameters
        // TODO authentication
        Map<String, Serializable> context = new HashMap<String, Serializable>();
        if (parameters != null) {
            context.putAll(parameters);
        }
        if (!context.containsKey("username")) {
            context.put("username", "Administrator");
        }
        try {
            session = CoreInstance.getInstance().open(
                    repository.repositoryName, context);
            rootFolder = new NuxeoFolder(session.getRootDocument(), this,
                    BaseRepository.ROOT_FOLDER_NAME);
        } catch (ClientException e) {
            throw new RuntimeException("Could not connect", e); // TODO
        }

        // preprocess type names for queries
        queryTypeNames = new HashMap<String, String>();
        queryPropNames = new HashMap<String, String>();
        for (Type type : repository.getTypes(null)) {
            String tname = type.getQueryName();
            String nxname;
            if (tname.equals(BaseType.FOLDER.getId().toLowerCase())) {
                nxname = "Folder";
            } else if (tname.equals(BaseType.DOCUMENT.getId().toLowerCase())) {
                nxname = "Document";
            } else {
                nxname = tname;
            }
            queryTypeNames.put(tname.toLowerCase(), nxname);
            for (PropertyDefinition pd : type.getPropertyDefinitions()) {
                String id = pd.getId();
                queryPropNames.put(id, id);
            }
        }
        // actual NXQL mapping for some CMIS system props
        queryPropNames.putAll(NuxeoProperty.propertyNameToNXQL);
    }

    public SPI getSPI() {
        return this;
    }

    public void close() {
        CoreInstance.getInstance().close(session);
    }

    public Repository getRepository() {
        return repository;
    }

    public Folder getRootFolder() {
        return rootFolder;
    }

    /*
     * ----- Factories -----
     */

    public ObjectId newObjectId(String id) {
        return new SimpleObjectId(id);
    }

    public ObjectEntry newObjectEntry(String typeId) {
        DocumentModel doc = new DocumentModelImpl(typeId);
        // doc = session.createDocumentModel(typeId);
        // doc.setPathInfo(((NuxeoObjectEntry) folder).doc.getPathAsString(),
        // "");
        return new NuxeoObjectEntry(doc, this);
    }

    private DocumentModel createDoc(String typeId, ObjectId folder,
            BaseType baseType) {
        if (typeId == null) {
            throw new IllegalArgumentException("Missing object type id");
        }
        Type type = repository.getType(typeId);
        if (type == null || type.getBaseType() != baseType) {
            throw new IllegalArgumentException(typeId);
        }
        DocumentModel doc;
        try {
            String typeName = ((NuxeoType) type).getNuxeoTypeName();
            doc = session.createDocumentModel(typeName);
        } catch (ClientException e) {
            throw new IllegalArgumentException(type.getId());
        }
        if (folder != null) {
            DocumentModel parentDoc;
            if (folder instanceof NuxeoFolder) {
                parentDoc = ((NuxeoFolder) folder).doc;
            } else {
                try {
                    parentDoc = session.getDocument(new IdRef(folder.getId()));
                } catch (ClientException e) {
                    throw new CMISRuntimeException("Cannot create object", e);
                }
            }
            doc.setPathInfo(parentDoc.getPathAsString(), typeId);
        }
        return doc;
    }

    public Document newDocument(String typeId, Folder folder) {
        return new NuxeoDocument(createDoc(typeId, folder, BaseType.DOCUMENT),
                this);
    }

    public Folder newFolder(String typeId, Folder folder) {
        return new NuxeoFolder(createDoc(typeId, folder, BaseType.FOLDER), this);
    }

    public Relationship newRelationship(String typeId) {
        return new NuxeoRelationship(createDoc(typeId, null,
                BaseType.RELATIONSHIP), this);
    }

    public Policy newPolicy(String typeId, Folder folder) {
        return new NuxeoPolicy(createDoc(typeId, folder, BaseType.POLICY), this);
    }

    /*
     * ----- Navigation Services -----
     */

    public List<ObjectEntry> getFolderTree(ObjectId folder, int depth,
            Inclusion inclusion) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public List<ObjectEntry> getDescendants(ObjectId folder, int depth,
            String orderBy, Inclusion inclusion) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ListPage<ObjectEntry> getChildren(ObjectId folder,
            Inclusion inclusion, String orderBy, Paging paging) {
        // TODO orderBy
        DocumentModelList docs;
        try {
            docs = session.getChildren(new IdRef(folder.getId()));
        } catch (ClientException e) {
            throw new RuntimeException(e.toString(), e); // TODO
        }
        if (docs == null) {
            throw new IllegalArgumentException(folder.getId());
        }
        List<ObjectEntry> all = new ArrayList<ObjectEntry>(docs.size());
        for (DocumentModel child : docs) {
            all.add(new NuxeoObjectEntry(child, this));
        }
        return SimpleConnection.getListPage(all, paging);
    }

    public ObjectEntry getFolderParent(ObjectId folder, String filter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<ObjectEntry> getObjectParents(ObjectId object,
            String filter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ListPage<ObjectEntry> getCheckedOutDocuments(ObjectId folder,
            Inclusion inclusion, Paging paging) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /*
     * ----- Object Services -----
     */

    protected NuxeoObjectEntry getObjectEntry(ObjectId object) {
        if (object instanceof NuxeoObjectEntry) {
            return (NuxeoObjectEntry) object;
        }
        try {
            DocumentRef docRef = new IdRef(object.getId());
            if (!session.exists(docRef)) {
                return null;
            }
            return new NuxeoObjectEntry(session.getDocument(docRef), this);
        } catch (ClientException e) {
            throw new CMISRuntimeException(e);
        }
    }

    public ObjectId createDocument(Map<String, Serializable> properties,
            ObjectId folder, ContentStream contentStream,
            VersioningState versioningState) {
        // TODO contentStream, versioningState
        if (folder == null) {
            throw new IllegalArgumentException("Missing folder");
        }
        return createObject(properties, folder, BaseType.DOCUMENT);
    }

    public ObjectId createFolder(Map<String, Serializable> properties,
            ObjectId folder) {
        if (folder == null) {
            throw new IllegalArgumentException("Missing folder");
        }
        return createObject(properties, folder, BaseType.FOLDER);
    }

    public ObjectId createRelationship(Map<String, Serializable> properties) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ObjectId createPolicy(Map<String, Serializable> properties,
            ObjectId folder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    protected ObjectId createObject(Map<String, Serializable> properties,
            ObjectId folder, BaseType baseType) {
        String typeId = (String) properties.get(Property.TYPE_ID);
        DocumentModel doc = createDoc(typeId, folder, baseType);
        NuxeoObjectEntry entry = new NuxeoObjectEntry(doc, this);
        // don't mutate properties -> cannot remove TYPE_ID
        for (Entry<String, Serializable> e : properties.entrySet()) {
            String key = e.getKey();
            if (Property.TYPE_ID.equals(key)) {
                continue;
            }
            entry.setValue(key, e.getValue());
        }
        try {
            doc = session.createDocument(doc);
            session.save();
        } catch (ClientException e) {
            throw new CMISRuntimeException("Cannot create folder", e);
        }
        return NuxeoObject.construct(doc, this);
    }

    public Collection<QName> getAllowableActions(ObjectId object) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ObjectEntry getProperties(ObjectId object, Inclusion inclusion) {
        // TODO filter, includeAllowableActions, includeRelationships
        return getObjectEntry(object);
    }

    public ObjectEntry getObjectByPath(String path, Inclusion inclusion) {
        // TODO Inclusion
        try {
            DocumentRef docRef = new PathRef(path);
            if (!session.exists(docRef)) {
                return null;
            }
            return new NuxeoObjectEntry(session.getDocument(docRef), this);
        } catch (ClientException e) {
            throw new RuntimeException(e); // TODO
        }
    }

    public Folder getFolder(String path) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public CMISObject getObject(ObjectId object) {
        DocumentModel doc;
        try {
            DocumentRef docRef = new IdRef(object.getId());
            if (!session.exists(docRef)) {
                return null;
            }
            doc = session.getDocument(docRef);
        } catch (ClientException e) {
            throw new RuntimeException(e); // TODO
        }
        switch (repository.getType(doc.getType()).getBaseType()) {
        case DOCUMENT:
            return new NuxeoDocument(doc, this);
        case FOLDER:
            return new NuxeoFolder(doc, this);
        case RELATIONSHIP:
            return new NuxeoRelationship(doc, this);
        case POLICY:
            return new NuxeoPolicy(doc, this);
        default:
            throw new AssertionError();
        }
    }

    public ListPage<Rendition> getRenditions(ObjectId object,
            Inclusion inclusion, Paging paging) {
        return SimpleListPage.emptyList();
    }

    public boolean hasContentStream(ObjectId document) {
        DocumentModel doc;
        try {
            doc = session.getDocument(new IdRef(document.getId()));
        } catch (ClientException e) {
            throw new RuntimeException("Not found: " + document.getId(), e); // TODO
        }
        if (doc == null) {
            throw new RuntimeException("Not found: " + document.getId()); // TODO
        }
        if (!doc.hasSchema("file")) {
            return false;
        }
        try {
            return doc.getProperty("file", "content") != null;
        } catch (ClientException e) {
            throw new RuntimeException(e.toString(), e); // TODO
        }
    }

    public ContentStream getContentStream(ObjectId object,
            String contentStreamId) throws IOException {
        // TODO contentStreamId
        DocumentModel doc;
        try {
            doc = session.getDocument(new IdRef(object.getId()));
        } catch (ClientException e) {
            throw new RuntimeException("Not found: " + object.getId(), e); // TODO
        }
        if (doc == null) {
            throw new RuntimeException("Not found: " + object.getId()); // TODO
        }
        return NuxeoProperty.extractContentStream(doc);
    }

    public ObjectId setContentStream(ObjectId document, boolean overwrite,
            ContentStream contentStream) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ObjectId deleteContentStream(ObjectId document) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ObjectId updateProperties(ObjectId object, String changeToken,
            Map<String, Serializable> properties) {
        // TODO changeToken
        ObjectEntry objectEntry = getObjectEntry(object);
        for (Entry<String, Serializable> en : properties.entrySet()) {
            objectEntry.setValue(en.getKey(), en.getValue());
        }
        return objectEntry;
    }

    public ObjectId moveObject(ObjectId object, ObjectId targetFolder,
            ObjectId sourceFolder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void deleteObject(ObjectId object, boolean allVersions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<ObjectId> deleteTree(ObjectId folder, Unfiling unfiling,
            boolean continueOnFailure) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void addObjectToFolder(ObjectId object, ObjectId folder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void removeObjectFromFolder(ObjectId object, ObjectId folder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /*
     * ----- Discovery Services -----
     */

    public ListPage<ObjectEntry> query(String statement,
            boolean searchAllVersions, Inclusion inclusion, Paging paging) {

        IterableQueryResult iterable;
        try {
            iterable = session.queryAndFetch(statement, CMISQLQueryMaker.TYPE,
                    this);
        } catch (ClientException e) {
            throw new RuntimeException(e.toString(), e);
        }

        iterable = getPolicyFilteredMaps(iterable);

        if (paging != null && paging.skipCount > 0) {
            iterable.skipTo(paging.skipCount);
        }
        Iterator<Map<String, Serializable>> it = iterable.iterator();

        int maxItems = paging == null || paging.maxItems == 0 ? -1
                : paging.maxItems;
        SimpleListPage<ObjectEntry> page = new SimpleListPage<ObjectEntry>();
        while (it.hasNext()) {
            Map<String, Serializable> next = it.next();
            SimpleData data = new SimpleData(null, null);
            // don't use putAll, null values are forbidden
            for (Entry<String, Serializable> entry : next.entrySet()) {
                Serializable value = entry.getValue();
                if (value != null) {
                    data.put(entry.getKey(), value);
                }
            }
            page.add(new SimpleObjectEntry(data, this));
            if (--maxItems == 0) {
                break;
            }
        }
        page.setHasMoreItems(it.hasNext());
        if (iterable instanceof ListQueryResult) {
            page.setNumItems(((ListQueryResult) iterable).size);
        } else {
            page.setNumItems(-1);
        }
        return page;
    }

    public Collection<CMISObject> query(String statement,
            boolean searchAllVersions) {
        IterableQueryResult iterable;
        try {
            iterable = session.queryAndFetch(statement, CMISQLQueryMaker.TYPE,
                    this);
        } catch (ClientException e) {
            throw new RuntimeException(e.toString(), e);
        }

        List<String> ids = getPolicyFilteredIds(iterable);
        List<CMISObject> results = new ArrayList<CMISObject>(ids.size());
        for (String id : ids) {
            DocumentModel doc;
            try {
                doc = session.getDocument(new IdRef(id));
            } catch (ClientException e) {
                log.error("Cannot fetch document: " + id, e);
                continue;
            }
            results.add(NuxeoObject.construct(doc, this));
        }
        return results;
    }

    /**
     * Restricts through policies and returns a list of ids.
     */
    protected List<String> getPolicyFilteredIds(IterableQueryResult iterable) {
        SecurityPolicyService service;
        try {
            service = Framework.getService(SecurityPolicyService.class);
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
        // restrict through policies
        String permission = SecurityConstants.BROWSE;
        boolean filterPolicies = service.arePoliciesRestrictingPermission(permission);
        List<String> ids = new ArrayList<String>();
        row: //
        for (Map<String, Serializable> map : iterable) {
            boolean gotId = false;
            for (Entry<String, Serializable> entry : map.entrySet()) {
                String key = entry.getKey(); // was canonicalized
                if (key.equals(Property.ID) || key.endsWith('.' + Property.ID)) {
                    // check security on all objects
                    // TODO don't check relations
                    String id = (String) entry.getValue();
                    if (filterPolicies) {
                        boolean ok;
                        try {
                            ok = session.hasPermission(new IdRef(id),
                                    permission);
                        } catch (ClientException e) {
                            log.error("Cannot fetch document: " + id, e);
                            ok = false;
                        }
                        if (!ok) {
                            // skip document for which policy restricts access
                            continue row;
                        }
                    }
                    if (!gotId) {
                        ids.add(id);
                        gotId = true;
                    }
                }
            }
        }
        return ids;
    }

    /**
     * Restricts through policies and returns an iterable of maps.
     */
    protected IterableQueryResult getPolicyFilteredMaps(
            IterableQueryResult iterable) {
        SecurityPolicyService service;
        try {
            service = Framework.getService(SecurityPolicyService.class);
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
        String permission = SecurityConstants.BROWSE;
        boolean filterPolicies = service.arePoliciesRestrictingPermission(permission);
        if (!filterPolicies) {
            return iterable;
        }
        List<Map<String, Serializable>> list = new ArrayList<Map<String, Serializable>>();
        row: //
        for (Map<String, Serializable> map : iterable) {
            for (Entry<String, Serializable> entry : map.entrySet()) {
                String key = entry.getKey(); // was canonicalized
                if (key.equals(Property.ID) || key.endsWith('.' + Property.ID)) {
                    // check security on all objects
                    // TODO don't check relations
                    String id = (String) entry.getValue();
                    boolean ok;
                    try {
                        ok = session.hasPermission(new IdRef(id), permission);
                    } catch (ClientException e) {
                        log.error("Cannot fetch document: " + id, e);
                        ok = false;
                    }
                    if (!ok) {
                        // skip document for which policy restricts access
                        continue row;
                    }
                }
            }
            list.add(map);
        }
        return new ListQueryResult(list);
    }

    /**
     * IterableQueryResult backed by a simple list.
     */
    public static class ListQueryResult implements IterableQueryResult {
        public final Iterator<Map<String, Serializable>> it;

        public final int size;

        public ListQueryResult(List<Map<String, Serializable>> list) {
            it = list.iterator();
            size = list.size();
        }

        public Iterator<Map<String, Serializable>> iterator() {
            return it;
        }

        public void skipTo(long skipCount) {
            for (int i = 0; i < skipCount; i++) {
                if (!it.hasNext()) {
                    break;
                }
                it.next();
            }
        }

        public void close() {
        }
    }

    public ListPage<ObjectEntry> getChangeLog(String changeLogToken,
            boolean includeProperties, Paging paging,
            String[] lastChangeLogToken) {
        lastChangeLogToken[0] = null;
        return SimpleListPage.emptyList();
    }

    /*
     * ----- Versioning Services -----
     */

    public ObjectId checkOut(ObjectId document, boolean[] contentCopied) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void cancelCheckOut(ObjectId document) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ObjectId checkIn(ObjectId document, boolean major,
            Map<String, Serializable> properties, ContentStream contentStream,
            String comment) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Map<String, Serializable> getPropertiesOfLatestVersion(
            String versionSeriesId, boolean major, String filter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<ObjectEntry> getAllVersions(String versionSeriesId,
            String filter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /*
     * ----- Relationship Services -----
     */

    public ListPage<ObjectEntry> getRelationships(ObjectId object,
            String typeId, boolean includeSubRelationshipTypes,
            Inclusion inclusion, Paging paging) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /*
     * ----- Policy Services -----
     */

    public void applyPolicy(ObjectId object, ObjectId policy) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void removePolicy(ObjectId object, ObjectId policy) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<ObjectEntry> getAppliedPolicies(ObjectId policy,
            String filter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /*
     * ----- ACL Services -----
     */

    public List<ACE> getACL(ObjectId object, boolean onlyBasicPermissions,
            boolean[] exact) {
        // TODO Auto-generated method stub
        return Collections.emptyList();
    }

    public List<ACE> applyACL(ObjectId object, List<ACE> addACEs,
            List<ACE> removeACEs, ACLPropagation propagation, boolean[] exact,
            String[] changeToken) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

}
