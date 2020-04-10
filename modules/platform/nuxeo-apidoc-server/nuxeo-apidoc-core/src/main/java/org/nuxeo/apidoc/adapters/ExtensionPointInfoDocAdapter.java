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

import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.QueryHelper;
import org.nuxeo.apidoc.api.VirtualNodesConsts;
import org.nuxeo.apidoc.documentation.DocumentationHelper;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.PropertyException;

public class ExtensionPointInfoDocAdapter extends BaseNuxeoArtifactDocAdapter implements ExtensionPointInfo {

    public static ExtensionPointInfoDocAdapter create(ExtensionPointInfo xpi, CoreSession session,
            String containerPath) {

        DocumentModel doc = session.createDocumentModel(TYPE_NAME);

        String name = computeDocumentName("xp-" + xpi.getId());
        String targetPath = new Path(containerPath).append(name).toString();
        boolean exist = false;
        if (session.exists(new PathRef(targetPath))) {
            exist = true;
            doc = session.getDocument(new PathRef(targetPath));
        }
        doc.setPathInfo(containerPath, name);
        doc.setPropertyValue(NuxeoArtifact.TITLE_PROPERTY_PATH, xpi.getId());

        doc.setPropertyValue(PROP_NAME, xpi.getName());
        doc.setPropertyValue(PROP_EP_ID, xpi.getId());
        doc.setPropertyValue(PROP_DOC, xpi.getDocumentation());
        // TODO incoherent naming here, also schema has no types
        doc.setPropertyValue(PROP_DESCRIPTORS, xpi.getDescriptors());

        if (exist) {
            doc = session.saveDocument(doc);
        } else {
            doc = session.createDocument(doc);
        }
        return new ExtensionPointInfoDocAdapter(doc);
    }

    public ExtensionPointInfoDocAdapter(DocumentModel doc) {
        super(doc);
    }

    @Override
    public ComponentInfo getComponent() {
        log.error("getComponent Not implemented");
        return null;
    }

    @Override
    public String getComponentId() {
        return getId().split("--")[0];
    }

    @Override
    public String getDocumentation() {
        return safeGet(PROP_DOC);
    }

    @Override
    public String getDocumentationHtml() {
        return DocumentationHelper.getHtml(getDocumentation());
    }

    @Override
    public List<ExtensionInfo> getExtensions() {
        List<ExtensionInfo> result = new ArrayList<>();
        // find root doc for distribution
        DocumentModel dist = doc;
        while (!DistributionSnapshot.TYPE_NAME.equals(dist.getType())) {
            dist = getCoreSession().getParentDocument(dist.getRef());
        }
        String query = QueryHelper.select(ExtensionInfo.TYPE_NAME, dist, ExtensionInfo.PROP_EXTENSION_POINT, getId());
        DocumentModelList docs = getCoreSession().query(query + QueryHelper.ORDER_BY_POS);
        for (DocumentModel contribDoc : docs) {
            ExtensionInfo contrib = contribDoc.getAdapter(ExtensionInfo.class);
            if (contrib != null) {
                result.add(contrib);
            }
        }
        return result;
    }

    @Override
    public String getName() {
        return safeGet(PROP_NAME);
    }

    @Override
    public String[] getDescriptors() {
        try {
            @SuppressWarnings("unchecked")
            List<String> descriptors = (List<String>) doc.getPropertyValue(PROP_DESCRIPTORS);
            return descriptors.toArray(new String[0]);
        } catch (PropertyException e) {
            log.error("Unable to get descriptors field", e);
        }
        return null;
    }

    @Override
    public String getId() {
        return safeGet(PROP_EP_ID);
    }

    @Override
    public String getVersion() {
        BundleInfo parentBundle = getParentNuxeoArtifact(BundleInfo.class);

        if (parentBundle != null) {
            return parentBundle.getVersion();
        }

        log.error("Unable to determine version for ExtensionPoint " + getId());
        return "?";
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public String getLabel() {
        return getName() + " (" + getComponent().getId() + ")";
    }

    @Override
    public String getHierarchyPath() {
        String path = super.getHierarchyPath() + "###";
        String toReplace = "/" + getId() + "###";
        return path.replace(toReplace, "/" + VirtualNodesConsts.ExtensionPoints_VNODE_NAME + "/" + getId());
    }

}
