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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.BaseType;
import org.apache.chemistry.CMISObject;
import org.apache.chemistry.Connection;
import org.apache.chemistry.ContentStream;
import org.apache.chemistry.Document;
import org.apache.chemistry.Folder;
import org.apache.chemistry.ObjectEntry;
import org.apache.chemistry.ObjectId;
import org.apache.chemistry.Policy;
import org.apache.chemistry.Relationship;
import org.apache.chemistry.RelationshipDirection;
import org.apache.chemistry.Repository;
import org.apache.chemistry.ReturnVersion;
import org.apache.chemistry.SPI;
import org.apache.chemistry.Type;
import org.apache.chemistry.Unfiling;
import org.apache.chemistry.VersioningState;
import org.apache.chemistry.impl.simple.SimpleObjectId;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;

public class NuxeoConnection implements Connection, SPI {

    protected final NuxeoRepository repository;

    protected final CoreSession session;

    protected final NuxeoFolder rootFolder;

    public NuxeoConnection(NuxeoRepository repository,
            Map<String, Serializable> parameters) {
        this.repository = repository;

        // TODO map parameters
        // TODO authentication
        Map<String, Serializable> context = new HashMap<String, Serializable>();
        context.put("username", "Administrator");
        try {
            session = CoreInstance.getInstance().open(
                    repository.repositoryName, context);
            rootFolder = new NuxeoFolder(session.getRootDocument(), this);
        } catch (ClientException e) {
            throw new RuntimeException("Could not connect", e); // TODO
        }
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

    private DocumentModel createDoc(String typeId, Folder folder) {
        DocumentModel doc;
        try {
            doc = session.createDocumentModel(typeId);
        } catch (ClientException e) {
            throw new IllegalArgumentException(typeId);
        }
        if (folder != null) {
            doc.setPathInfo(((NuxeoFolder) folder).doc.getPathAsString(), "");
        }
        return doc;
    }

    public Document newDocument(String typeId, Folder folder) {
        Type type = repository.getType(typeId);
        if (type == null || type.getBaseType() != BaseType.DOCUMENT) {
            throw new IllegalArgumentException(typeId);
        }
        return new NuxeoDocument(createDoc(typeId, folder), this);
    }

    public Folder newFolder(String typeId, Folder folder) {
        Type type = repository.getType(typeId);
        if (type == null || type.getBaseType() != BaseType.FOLDER) {
            throw new IllegalArgumentException(typeId);
        }
        return new NuxeoFolder(createDoc(typeId, folder), this);
    }

    public Relationship newRelationship(String typeId) {
        Type type = repository.getType(typeId);
        if (type == null || type.getBaseType() != BaseType.RELATIONSHIP) {
            throw new IllegalArgumentException(typeId);
        }
        return new NuxeoRelationship(createDoc(typeId, null), this);
    }

    public Policy newPolicy(String typeId, Folder folder) {
        Type type = repository.getType(typeId);
        if (type == null || type.getBaseType() != BaseType.POLICY) {
            throw new IllegalArgumentException(typeId);
        }
        return new NuxeoPolicy(createDoc(typeId, folder), this);
    }

    /*
     * ----- Navigation Services -----
     */

    public List<ObjectEntry> getDescendants(ObjectId folder, BaseType type,
            int depth, String filter, boolean includeAllowableActions,
            boolean includeRelationships, String orderBy) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public List<ObjectEntry> getChildren(ObjectId folder, BaseType type,
            String filter, boolean includeAllowableActions,
            boolean includeRelationships, int maxItems, int skipCount,
            String orderBy, boolean[] hasMoreItems) {
        // TODO type and orderBy
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

        int fromIndex = skipCount;
        if (fromIndex < 0 || fromIndex > all.size()) {
            hasMoreItems[0] = false;
            return Collections.emptyList();
        }
        if (maxItems == 0) {
            maxItems = all.size();
        }
        int toIndex = skipCount + maxItems;
        if (toIndex > all.size()) {
            toIndex = all.size();
        }
        hasMoreItems[0] = toIndex < all.size();
        return all.subList(fromIndex, toIndex);
    }

    public List<ObjectEntry> getFolderParent(ObjectId folder, String filter,
            boolean includeAllowableActions, boolean includeRelationships,
            boolean returnToRoot) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<ObjectEntry> getObjectParents(ObjectId object,
            String filter, boolean includeAllowableActions,
            boolean includeRelationships) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<ObjectEntry> getCheckedoutDocuments(ObjectId folder,
            String filter, boolean includeAllowableActions,
            boolean includeRelationships, int maxItems, int skipCount,
            boolean[] hasMoreItems) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /*
     * ----- Object Services -----
     */

    public ObjectId createDocument(String typeId,
            Map<String, Serializable> properties, ObjectId folder,
            ContentStream contentStream, VersioningState versioningState) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ObjectId createFolder(String typeId,
            Map<String, Serializable> properties, ObjectId folder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ObjectId createRelationship(String typeId,
            Map<String, Serializable> properties, ObjectId source,
            ObjectId target) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ObjectId createPolicy(String typeId,
            Map<String, Serializable> properties, ObjectId folder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<String> getAllowableActions(ObjectId object, String asUser) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ObjectEntry getProperties(ObjectId object,
            ReturnVersion returnVersion, String filter,
            boolean includeAllowableActions, boolean includeRelationships) {
        // TODO filter, includeAllowableActions, includeRelationships
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
        return new NuxeoObjectEntry(doc, this);
    }

    public CMISObject getObject(ObjectId object, ReturnVersion returnVersion) {
        // TODO returnVersion
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

    public InputStream getContentStream(ObjectId document, int offset,
            int length) throws IOException {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset: " + offset);
        }
        if (length < 0) {
            throw new IllegalArgumentException("Length: " + offset);
        }
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
            return null;
        }
        Blob blob;
        try {
            blob = (Blob) doc.getProperty("file", "content");
        } catch (ClientException e) {
            throw new RuntimeException(e.toString(), e); // TODO
        }
        if (blob == null) {
            return null;
        }
        InputStream stream = blob.getStream();

        // offset, length
        long skipped = stream.skip(offset);
        if (skipped < offset) {
            // assume we reached EOF
            stream = new ByteArrayInputStream(new byte[0]);
        }
        // XXX TODO length
        return stream;
    }

    public ObjectId setContentStream(ObjectId document, boolean overwrite,
            ContentStream contentStream) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void deleteContentStream(ObjectId document) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ObjectId updateProperties(ObjectId objeId, String changeToken,
            Map<String, Serializable> properties) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void moveObject(ObjectId object, ObjectId targetFolder,
            ObjectId sourceFolder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void moveObject(CMISObject object, Folder targetFolder,
            Folder sourceFolder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void deleteObject(ObjectId object) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void deleteObject(CMISObject object) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<String> deleteTree(ObjectId folder, Unfiling unfiling,
            boolean continueOnFailure) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<String> deleteTree(Folder folder, Unfiling unfiling,
            boolean continueOnFailure) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void addObjectToFolder(ObjectId object, ObjectId folder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void addObjectToFolder(CMISObject object, Folder folder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void removeObjectFromFolder(ObjectId object, ObjectId folder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void removeObjectFromFolder(CMISObject object, Folder folder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /*
     * ----- Discovery Services -----
     */

    public Collection<ObjectEntry> query(String statement,
            boolean searchAllVersions, boolean includeAllowableActions,
            boolean includeRelationships, int maxItems, int skipCount,
            boolean[] hasMoreItems) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<CMISObject> query(String statement,
            boolean searchAllVersions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /*
     * ----- Versioning Services -----
     */

    public ObjectId checkOut(ObjectId document, boolean[] contentCopied) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Document checkOut(Document document) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void cancelCheckOut(ObjectId document) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void cancelCheckOut(Document document) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ObjectId checkIn(ObjectId document, boolean major,
            Map<String, Serializable> properties, ContentStream contentStream,
            String comment) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Document checkIn(Document document, boolean major, String comment) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Map<String, Serializable> getPropertiesOfLatestVersion(
            String versionSeriesId, boolean majorVersion, String filter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Document getLatestVersion(Document document, boolean major) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<ObjectEntry> getAllVersions(String versionSeriesId,
            String filter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<Document> getAllVersions(Document document, String filter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void deleteAllVersions(String versionSeriesId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void deleteAllVersions(Document document) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /*
     * ----- Relationship Services -----
     */

    public List<ObjectEntry> getRelationships(ObjectId object,
            RelationshipDirection direction, String typeId,
            boolean includeSubRelationshipTypes, String filter,
            String includeAllowableActions, int maxItems, int skipCount,
            boolean[] hasMoreItems) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public List<Relationship> getRelationships(CMISObject object,
            RelationshipDirection direction, String typeId,
            boolean includeSubRelationshipTypes) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /*
     * ----- Policy Services -----
     */

    public void applyPolicy(ObjectId policy, ObjectId object) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void applyPolicy(Policy policy, CMISObject object) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void removePolicy(ObjectId policy, ObjectId object) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void removePolicy(Policy policy, CMISObject object) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<ObjectEntry> getAppliedPolicies(ObjectId policy,
            String filter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<Policy> getAppliedPolicies(CMISObject object) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

}
