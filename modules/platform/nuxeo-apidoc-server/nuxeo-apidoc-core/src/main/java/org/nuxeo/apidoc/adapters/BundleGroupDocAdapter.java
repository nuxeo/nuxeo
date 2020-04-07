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
package org.nuxeo.apidoc.adapters;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.QueryHelper;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;

public class BundleGroupDocAdapter extends BaseNuxeoArtifactDocAdapter implements BundleGroup {

    public static BundleGroupDocAdapter create(BundleGroup bundleGroup, CoreSession session, String containerPath) {

        DocumentModel doc = session.createDocumentModel(TYPE_NAME);
        String name = computeDocumentName("bg-" + bundleGroup.getId());
        String targetPath = new Path(containerPath).append(name).toString();

        boolean exist = false;
        if (session.exists(new PathRef(targetPath))) {
            exist = true;
            doc = session.getDocument(new PathRef(targetPath));
        }
        doc.setPathInfo(containerPath, name);
        doc.setPropertyValue(NuxeoArtifact.TITLE_PROPERTY_PATH, bundleGroup.getName());
        doc.setPropertyValue(PROP_GROUP_NAME, bundleGroup.getName());
        doc.setPropertyValue(PROP_KEY, bundleGroup.getId());
        if (exist) {
            doc = session.saveDocument(doc);
        } else {
            doc = session.createDocument(doc);
        }
        return new BundleGroupDocAdapter(doc);
    }

    public BundleGroupDocAdapter(DocumentModel doc) {
        super(doc);
    }

    @Override
    public List<String> getBundleIds() {
        List<String> bundles = new ArrayList<>();
        String query = QueryHelper.select(BundleInfo.TYPE_NAME, doc);
        DocumentModelList docs = getCoreSession().query(query + QueryHelper.ORDER_BY_POS);
        for (DocumentModel child : docs) {
            BundleInfo bi = child.getAdapter(BundleInfo.class);
            if (bi != null && !bi.getId().equals(getId())) {
                bundles.add(bi.getId());
            }
        }
        return bundles;
    }

    private String getKey() {
        return safeGet(PROP_KEY, "unknown_bundle_group");
    }

    @Override
    public String getName() {
        return safeGet(PROP_GROUP_NAME, "unknown_bundle_group");
    }

    @Override
    public List<BundleGroup> getSubGroups() {
        List<BundleGroup> grps = new ArrayList<>();
        String query = QueryHelper.select(TYPE_NAME, doc);
        DocumentModelList docs = getCoreSession().query(query + QueryHelper.ORDER_BY_POS);
        for (DocumentModel child : docs) {
            BundleGroup grp = child.getAdapter(BundleGroup.class);
            if (grp != null) {
                grps.add(grp);
            }
        }
        return grps;
    }

    @Override
    public String getId() {
        return getKey();
    }

    @Override
    public String getVersion() {
        DistributionSnapshot parentSnapshot = getParentNuxeoArtifact(DistributionSnapshot.class);

        if (parentSnapshot == null) {
            log.error("Unable to determine version for bundleGroup " + getId());
            return "?";
        }
        return parentSnapshot.getVersion();
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public List<String> getParentIds() {
        throw new UnsupportedOperationException();
    }

}
