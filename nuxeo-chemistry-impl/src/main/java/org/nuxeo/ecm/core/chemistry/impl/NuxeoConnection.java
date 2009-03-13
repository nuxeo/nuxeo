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

import org.apache.chemistry.CMISObject;
import org.apache.chemistry.Connection;
import org.apache.chemistry.ContentStream;
import org.apache.chemistry.Document;
import org.apache.chemistry.Folder;
import org.apache.chemistry.ObjectEntry;
import org.apache.chemistry.Policy;
import org.apache.chemistry.Relationship;
import org.apache.chemistry.RelationshipDirection;
import org.apache.chemistry.ReturnVersion;
import org.apache.chemistry.SPI;
import org.apache.chemistry.Unfiling;
import org.apache.chemistry.VersioningState;
import org.apache.chemistry.repository.Repository;
import org.apache.chemistry.type.BaseType;
import org.apache.chemistry.type.Type;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;

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
            String rootFolderId = session.getRootDocument().getId();
            rootFolder = (NuxeoFolder) getObject(rootFolderId,
                    ReturnVersion.THIS);
        } catch (ClientException e) {
            throw new RuntimeException("Could not connect", e); // TODO
        }
    }

    public Connection getConnection() {
        return this;
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

    private DocumentModel createDoc(String typeId, ObjectEntry folder) {
        DocumentModel doc;
        try {
            doc = session.createDocumentModel(typeId);
        } catch (ClientException e) {
            throw new IllegalArgumentException(typeId);
        }
        if (folder != null) {
            doc.setPathInfo(((NuxeoObjectEntry) folder).doc.getPathAsString(),
                    "");
        }
        return doc;
    }

    public Document newDocument(String typeId, ObjectEntry folder) {
        Type type = repository.getType(typeId);
        if (type == null || type.getBaseType() != BaseType.DOCUMENT) {
            throw new IllegalArgumentException(typeId);
        }
        return new NuxeoDocument(createDoc(typeId, folder), this);
    }

    public Folder newFolder(String typeId, ObjectEntry folder) {
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

    public Policy newPolicy(String typeId, ObjectEntry folder) {
        Type type = repository.getType(typeId);
        if (type == null || type.getBaseType() != BaseType.POLICY) {
            throw new IllegalArgumentException(typeId);
        }
        return new NuxeoPolicy(createDoc(typeId, folder), this);
    }

    /*
     * ----- Navigation Services -----
     */

    public List<ObjectEntry> getDescendants(String folderId, BaseType type,
            int depth, String filter, boolean includeAllowableActions,
            boolean includeRelationships, String orderBy) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public List<ObjectEntry> getChildren(String folderId, BaseType type,
            String filter, boolean includeAllowableActions,
            boolean includeRelationships, int maxItems, int skipCount,
            String orderBy, boolean[] hasMoreItems) {
        // TODO type and orderBy
        DocumentModelList docs;
        try {
            docs = session.getChildren(new IdRef(folderId));
        } catch (ClientException e) {
            throw new RuntimeException(e.toString(), e); // TODO
        }
        if (docs == null) {
            throw new IllegalArgumentException(folderId);
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

    public List<ObjectEntry> getFolderParent(String folderId, String filter,
            boolean includeAllowableActions, boolean includeRelationships,
            boolean returnToRoot) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<ObjectEntry> getObjectParents(String objectId,
            String filter, boolean includeAllowableActions,
            boolean includeRelationships) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<ObjectEntry> getCheckedoutDocuments(String folderId,
            String filter, boolean includeAllowableActions,
            boolean includeRelationships, int maxItems, int skipCount,
            boolean[] hasMoreItems) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /*
     * ----- Object Services -----
     */

    public String createDocument(String typeId,
            Map<String, Serializable> properties, String folderId,
            ContentStream contentStream, VersioningState versioningState) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public String createFolder(String typeId,
            Map<String, Serializable> properties, String folderId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public String createRelationship(String typeId,
            Map<String, Serializable> properties, String sourceId,
            String targetId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public String createPolicy(String typeId,
            Map<String, Serializable> properties, String folderId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<String> getAllowableActions(String objectId, String asUser) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ObjectEntry getProperties(String objectId,
            ReturnVersion returnVersion, String filter,
            boolean includeAllowableActions, boolean includeRelationships) {
        // TODO filter, includeAllowableActions, includeRelationships
        return getObject(objectId, returnVersion);
    }

    public CMISObject getObject(String objectId, ReturnVersion returnVersion) {
        // TODO returnVersion
        DocumentModel doc;
        try {
            doc = session.getDocument(new IdRef(objectId));
        } catch (ClientException e) {
            throw new RuntimeException("Not found: " + objectId, e); // TODO
        }
        if (doc == null) {
            throw new RuntimeException("Not found: " + objectId); // TODO
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
            throw new RuntimeException();
        }
    }

    public InputStream getContentStream(String documentId, int offset,
            int length) throws IOException {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset: " + offset);
        }
        if (length < 0) {
            throw new IllegalArgumentException("Length: " + offset);
        }
        DocumentModel doc;
        try {
            doc = session.getDocument(new IdRef(documentId));
        } catch (ClientException e) {
            throw new RuntimeException("Not found: " + documentId, e); // TODO
        }
        if (doc == null) {
            throw new RuntimeException("Not found: " + documentId); // TODO
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

    public void setContentStream(String documentId, boolean overwrite,
            ContentStream contentStream) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void deleteContentStream(String documentId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public String updateProperties(String objectId, String changeToken,
            Map<String, Serializable> properties) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void moveObject(String objectId, String targetFolderId,
            String sourceFolderId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void moveObject(ObjectEntry object, ObjectEntry targetFolder,
            ObjectEntry sourceFolder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void deleteObject(String objectId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void deleteObject(ObjectEntry object) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<String> deleteTree(String folderId, Unfiling unfiling,
            boolean continueOnFailure) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<String> deleteTree(ObjectEntry folder, Unfiling unfiling,
            boolean continueOnFailure) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void addObjectToFolder(String objectId, String folderId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void addObjectToFolder(ObjectEntry object, ObjectEntry folder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void removeObjectFromFolder(String objectId, String folderId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void removeObjectFromFolder(ObjectEntry object, ObjectEntry folder) {
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

    public Collection<ObjectEntry> query(String statement,
            boolean searchAllVersions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /*
     * ----- Versioning Services -----
     */

    public String checkOut(String documentId, boolean[] contentCopied) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public CMISObject checkOut(ObjectEntry document) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void cancelCheckOut(String documentId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void cancelCheckOut(ObjectEntry document) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public String checkIn(String documentId, boolean major,
            Map<String, Serializable> properties, ContentStream contentStream,
            String comment) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public CMISObject checkIn(ObjectEntry document, boolean major,
            String comment) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Map<String, Serializable> getPropertiesOfLatestVersion(
            String versionSeriesId, boolean majorVersion, String filter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public CMISObject getLatestVersion(ObjectEntry document, boolean major) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<ObjectEntry> getAllVersions(String versionSeriesId,
            String filter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<ObjectEntry> getAllVersions(ObjectEntry document,
            String filter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void deleteAllVersions(String versionSeriesId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void deleteAllVersions(ObjectEntry document) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /*
     * ----- Relationship Services -----
     */

    public List<ObjectEntry> getRelationships(String objectId,
            RelationshipDirection direction, String typeId,
            boolean includeSubRelationshipTypes, String filter,
            String includeAllowableActions, int maxItems, int skipCount,
            boolean[] hasMoreItems) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public List<ObjectEntry> getRelationships(ObjectEntry object,
            RelationshipDirection direction, String typeId,
            boolean includeSubRelationshipTypes) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /*
     * ----- Policy Services -----
     */

    public void applyPolicy(String policyId, String objectId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void applyPolicy(Policy policy, ObjectEntry object) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void removePolicy(String policyId, String objectId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void removePolicy(Policy policy, ObjectEntry object) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<ObjectEntry> getAppliedPolicies(String policyId,
            String filter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<Policy> getAppliedPolicies(ObjectEntry object) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

}
