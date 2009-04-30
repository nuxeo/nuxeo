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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.Connection;
import org.apache.chemistry.JoinCapability;
import org.apache.chemistry.ObjectId;
import org.apache.chemistry.QueryCapability;
import org.apache.chemistry.Repository;
import org.apache.chemistry.RepositoryCapabilities;
import org.apache.chemistry.RepositoryEntry;
import org.apache.chemistry.RepositoryInfo;
import org.apache.chemistry.SPI;
import org.apache.chemistry.Type;
import org.apache.chemistry.impl.simple.SimpleObjectId;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;

public class NuxeoRepository implements Repository, RepositoryInfo,
        RepositoryCapabilities {

    protected final String repositoryName;

    protected final Map<String, Type> types;

    protected final ObjectId rootFolderId;

    public NuxeoRepository(String repositoryName) {
        this.repositoryName = repositoryName;

        SchemaManager schemaManager;
        try {
            schemaManager = Framework.getService(SchemaManager.class);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e.toString(), e); // TODO
        }
        types = new HashMap<String, Type>();
        for (DocumentType dt : schemaManager.getDocumentTypes()) {
            NuxeoType type = new NuxeoType(dt);
            types.put(type.getId(), type);
        }

        // initialize root folder id
        Connection conn = getConnection(null);
        try {
            rootFolderId = new SimpleObjectId(conn.getRootFolder().getId());
        } finally {
            conn.close();
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

    public URI getURI() {
        // TODO Return a URI
        return null;
    }

    public String getRelationshipName() {
        return null;
    }

    /*
     * ----- Repository -----
     */

    public Connection getConnection(Map<String, Serializable> parameters) {
        return new NuxeoConnection(this, parameters);
    }

    public SPI getSPI() {
        return new NuxeoConnection(this, null);
    }

    public <T> T getExtension(Class<T> klass) {
        return null; // not supported
    }

    public RepositoryInfo getInfo() {
        return this;
    }

    public Type getType(String typeId) {
        return types.get(typeId);
    }

    public Collection<Type> getTypes(String typeId,
            boolean returnPropertyDefinitions) {
        // TODO always returns property definitions for now
        if (typeId == null) {
            return Collections.unmodifiableCollection(types.values());
        }
        if (!types.containsKey(typeId)) {
            return null; // TODO
        }
        // TODO return all descendants as well
        return Collections.singleton(types.get(typeId));
    }

    public List<Type> getTypes(String typeId,
            boolean returnPropertyDefinitions, int maxItems, int skipCount,
            boolean[] hasMoreItems) {
        if (maxItems < 0) {
            throw new IllegalArgumentException(String.valueOf(maxItems));
        }
        if (skipCount < 0) {
            throw new IllegalArgumentException(String.valueOf(skipCount));
        }
        if (hasMoreItems.length < 1) {
            throw new IllegalArgumentException(
                    "hasMoreItems parameter too small");
        }

        Collection<Type> t = getTypes(typeId, returnPropertyDefinitions);
        if (t == null) {
            hasMoreItems[0] = false;
            return Collections.emptyList();
        }
        List<Type> all = new ArrayList<Type>(t);
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

    /*
     * ----- RepositoryInfo -----
     */

    public String getDescription() {
        return "Repository " + repositoryName;
    }

    public ObjectId getRootFolderId() {
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
        return "5.2-SNAPSHOT";
    }

    public String getVersionSupported() {
        // TODO may be overriden by generic client layer
        return "0.50";
    }

    public org.w3c.dom.Document getRepositorySpecificInformation() {
        return null;
    }

    public RepositoryCapabilities getCapabilities() {
        return this;
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

    public JoinCapability getJoinCapability() {
        return JoinCapability.INNER_AND_OUTER;
    }

    public QueryCapability getQueryCapability() {
        return QueryCapability.BOTH_COMBINED;
    }

}
