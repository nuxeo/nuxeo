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

import java.io.Serializable;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.chemistry.CapabilityACL;
import org.apache.chemistry.ACLCapabilityType;
import org.apache.chemistry.BaseType;
import org.apache.chemistry.CapabilityChange;
import org.apache.chemistry.Connection;
import org.apache.chemistry.CapabilityJoin;
import org.apache.chemistry.ListPage;
import org.apache.chemistry.ObjectId;
import org.apache.chemistry.CapabilityQuery;
import org.apache.chemistry.CapabilityRendition;
import org.apache.chemistry.Paging;
import org.apache.chemistry.PropertyDefinition;
import org.apache.chemistry.Repository;
import org.apache.chemistry.RepositoryCapabilities;
import org.apache.chemistry.RepositoryEntry;
import org.apache.chemistry.RepositoryInfo;
import org.apache.chemistry.SPI;
import org.apache.chemistry.Type;
import org.apache.chemistry.TypeManager;
import org.apache.chemistry.impl.simple.SimpleObjectId;
import org.apache.chemistry.impl.simple.SimpleTypeManager;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;

public class NuxeoRepository implements Repository, RepositoryInfo,
        RepositoryCapabilities {

    protected final String repositoryName;

    protected TypeManager typeManager;

    protected ObjectId rootFolderId;

    public NuxeoRepository(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    protected void initializeTypes() {
        if (typeManager != null) {
            return;
        }
        SchemaManager schemaManager;
        try {
            schemaManager = Framework.getService(SchemaManager.class);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e.toString(), e); // TODO
        }
        // scan the types to find super/inherited relationships
        Map<String, List<String>> typesChildren = new HashMap<String, List<String>>();
        for (DocumentType dt : schemaManager.getDocumentTypes()) {
            org.nuxeo.ecm.core.schema.types.Type st = dt.getSuperType();
            if (st == null) {
                continue;
            }
            String name = st.getName();
            List<String> siblings = typesChildren.get(name);
            if (siblings == null) {
                siblings = new LinkedList<String>();
                typesChildren.put(name, siblings);
            }
            siblings.add(dt.getName());
        }
        // convert the transitive closure for Folder and Document subtypes
        Set<String> done = new HashSet<String>();
        typeManager = new SimpleTypeManager();
        addTypeRecursively("Folder", typesChildren, done, schemaManager);
        addTypeRecursively("Document", typesChildren, done, schemaManager);
    }

    protected void addTypeRecursively(String name,
            Map<String, List<String>> typesChildren, Set<String> done,
            SchemaManager schemaManager) {
        if (done.contains(name)) {
            return;
        }
        done.add(name);
        DocumentType dt = schemaManager.getDocumentType(name);
        typeManager.addType(new NuxeoType(dt));
        // recurse in children
        List<String> children = typesChildren.get(name);
        if (children == null) {
            return;
        }
        for (String sub : children) {
            addTypeRecursively(sub, typesChildren, done, schemaManager);
        }
    }

    /*
     * ----- RepositoryEntry -----
     */

    public String getId() {
        return repositoryName;
    }

    public String getName() {
        return repositoryName;
    }

    public String getRelationshipName() {
        return null;
    }

    public URI getThinClientURI() {
        return null; // TODO
    }

    /*
     * ----- Repository -----
     */

    public Connection getConnection(Map<String, Serializable> params) {
        return new NuxeoConnection(this, params);
    }

    public SPI getSPI(Map<String, Serializable> params) {
        return new NuxeoConnection(this, params);
    }

    public <T> T getExtension(Class<T> klass) {
        return null; // not supported
    }

    public RepositoryInfo getInfo() {
        return this;
    }

    public void addType(Type type) {
        throw new UnsupportedOperationException("Cannot add types");
    }

    public Type getType(String typeId) {
        initializeTypes();
        return typeManager.getType(typeId);
    }

    public PropertyDefinition getPropertyDefinition(String id) {
        initializeTypes();
        return typeManager.getPropertyDefinition(id);
    }

    public Collection<Type> getTypes() {
        initializeTypes();
        return typeManager.getTypes();
    }

    public Collection<Type> getTypeDescendants(String typeId) {
        initializeTypes();
        return typeManager.getTypeDescendants(typeId);
    }

    public ListPage<Type> getTypeChildren(String typeId,
            boolean includePropertyDefinitions, Paging paging) {
        initializeTypes();
        return typeManager.getTypeChildren(typeId, includePropertyDefinitions,
                paging);
    }

    public Collection<Type> getTypeDescendants(String typeId, int depth,
            boolean includePropertyDefinitions) {
        initializeTypes();
        return typeManager.getTypeDescendants(typeId, depth,
                includePropertyDefinitions);
    }

    /*
     * ----- RepositoryInfo -----
     */

    public String getDescription() {
        return "Repository " + repositoryName;
    }

    public ObjectId getRootFolderId() {
        if (rootFolderId == null) {
            // lazy initialization to delay first connection
            Connection conn = getConnection(null);
            try {
                rootFolderId = new SimpleObjectId(conn.getRootFolder().getId());
            } finally {
                conn.close();
            }
        }
        return rootFolderId;
    }

    public String getVendorName() {
        return "Nuxeo";
    }

    public String getProductName() {
        return "Nuxeo Repository";
    }

    public String getProductVersion() {
        // TODO update this when releasing
        return "5.3.1-SNAPSHOT";
    }

    public String getVersionSupported() {
        // TODO may be overriden by generic client layer
        return "1.0";
    }

    public org.w3c.dom.Document getRepositorySpecificInformation() {
        return null;
    }

    public RepositoryCapabilities getCapabilities() {
        return this;
    }

    public Set<BaseType> getChangeLogBaseTypes() {
        // TODO-0.63 TCK checks 0.62 schema which has minOccurs=1
        Set<BaseType> changeLogBaseTypes = new HashSet<BaseType>();
        changeLogBaseTypes.add(BaseType.DOCUMENT);
        changeLogBaseTypes.add(BaseType.FOLDER);
        changeLogBaseTypes.add(BaseType.RELATIONSHIP);
        changeLogBaseTypes.add(BaseType.POLICY);
        return changeLogBaseTypes;
    }

    public boolean isChangeLogIncomplete() {
        return false;
    }

    public String getLatestChangeLogToken() {
        return "";
    }

    public ACLCapabilityType getACLCapabilityType() {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<RepositoryEntry> getRelatedRepositories() {
        return Collections.emptySet();
    }

    /*
     * ----- RepositoryCapabilities -----
     */

    public boolean hasMultifiling() {
        return false;
    }

    public boolean hasUnfiling() {
        return false;
    }

    public boolean hasVersionSpecificFiling() {
        return false;
    }

    public boolean isPWCUpdatable() {
        return true;
    }

    public boolean isPWCSearchable() {
        return true;
    }

    public boolean isAllVersionsSearchable() {
        return true;
    }

    public boolean hasGetDescendants() {
        return true;
    }

    public boolean hasGetFolderTree() {
        return true;
    }

    public boolean isContentStreamUpdatableAnytime() {
        return true;
    }

    public CapabilityJoin getJoinCapability() {
        return CapabilityJoin.INNER_AND_OUTER;
    }

    public CapabilityQuery getQueryCapability() {
        return CapabilityQuery.BOTH_COMBINED;
    }

    public CapabilityRendition getRenditionCapability() {
        return CapabilityRendition.NONE;
    }

    public CapabilityChange getChangeCapability() {
        return CapabilityChange.NONE;
    }

    public CapabilityACL getACLCapability() {
        return CapabilityACL.NONE;
    }

}
