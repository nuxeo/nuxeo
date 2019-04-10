/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.introspection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.documentation.AssociatedDocumentsImpl;
import org.nuxeo.apidoc.documentation.ResourceDocumentationItem;
import org.nuxeo.ecm.core.api.CoreSession;

public class BundleGroupImpl extends BaseNuxeoArtifact implements BundleGroup {

    protected final String key;

    protected final String name;

    protected final List<BundleGroup> subGroups = new ArrayList<BundleGroup>();

    protected final List<String> bundleIds = new ArrayList<String>();

    protected final String version;

    protected final List<String> parentIds = new ArrayList<String>();

    protected Map<String, ResourceDocumentationItem> liveDoc;

    public BundleGroupImpl(String key, String version) {
        this.key = key;
        if (key.startsWith("grp:")) {
            name = key.substring(4);
        } else {
            name = key;
        }
        this.version = version;
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
            liveDoc = new HashMap<String, ResourceDocumentationItem>();
        }
        if (newLiveDoc != null) {
            for (String key : newLiveDoc.keySet()) {
                if (newLiveDoc.get(key) != null) {
                    liveDoc.put(key,
                            new ResourceDocumentationItem(newLiveDoc.get(key),
                                    this));
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
