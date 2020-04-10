/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.apidoc.introspection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.documentation.AssociatedDocumentsImpl;
import org.nuxeo.apidoc.documentation.ResourceDocumentationItem;
import org.nuxeo.ecm.core.api.CoreSession;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BundleGroupImpl extends BaseNuxeoArtifact implements BundleGroup {

    protected final String key;

    protected final String name;

    protected final List<BundleGroup> subGroups = new ArrayList<>();

    protected final List<String> bundleIds = new ArrayList<>();

    protected final String version;

    protected final List<String> parentIds = new ArrayList<>();

    protected Map<String, ResourceDocumentationItem> liveDoc = new HashMap<>();

    @JsonCreator
    private BundleGroupImpl(@JsonProperty("id") String key, @JsonProperty("name") String version,
            @JsonProperty("liveDoc") Map<String, ResourceDocumentationItem> liveDoc) {
        this.key = key;
        if (key.startsWith("grp:")) {
            name = key.substring(4);
        } else {
            name = key;
        }
        this.version = version;
        this.liveDoc.putAll(liveDoc);
    }

    public BundleGroupImpl(String key, String version) {
        this(key, version, Collections.emptyMap());
    }

    void addParent(String bgId) {
        parentIds.add(bgId);
    }

    public String getKey() {
        return key;
    }

    @Override
    public String getName() {
        return name;
    }

    public void add(BundleGroupImpl group) {
        subGroups.add(group);
    }

    public void add(String bundleId) {
        bundleIds.add(bundleId);
    }

    @Override
    public List<BundleGroup> getSubGroups() {
        return subGroups;
    }

    @Override
    public List<String> getBundleIds() {
        return bundleIds;
    }

    @Override
    public List<String> getParentIds() {
        return parentIds;
    }

    @Override
    public String getId() {
        return key;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public String getHierarchyPath() {
        String path = "";
        for (String parentId : parentIds) {
            path = path + "/" + parentId;
        }
        return path + "/" + getId();
    }

    public void addLiveDoc(Map<String, ResourceDocumentationItem> newLiveDoc) {
        if (liveDoc == null) {
            liveDoc = new HashMap<>();
        }
        if (newLiveDoc != null) {
            for (String key : newLiveDoc.keySet()) {
                if (newLiveDoc.get(key) != null) {
                    liveDoc.put(key, new ResourceDocumentationItem(newLiveDoc.get(key), this));
                }
            }
        }
    }

    @Override
    public AssociatedDocumentsImpl getAssociatedDocuments(CoreSession session) {
        AssociatedDocumentsImpl docs = super.getAssociatedDocuments(session);
        if (liveDoc != null) {
            docs.setLiveDoc(liveDoc);
        }
        return docs;
    }

    public Map<String, ResourceDocumentationItem> getLiveDoc() {
        return liveDoc;
    }

}
