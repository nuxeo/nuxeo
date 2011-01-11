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
package org.nuxeo.apidoc.adapters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.QueryHelper;
import org.nuxeo.apidoc.api.VirtualNodesConsts;
import org.nuxeo.apidoc.documentation.DocumentationHelper;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;

public class ExtensionPointInfoDocAdapter extends BaseNuxeoArtifactDocAdapter
        implements ExtensionPointInfo {

    public static ExtensionPointInfoDocAdapter create(ExtensionPointInfo xpi,
            CoreSession session, String containerPath) throws Exception {

        DocumentModel doc = session.createDocumentModel(TYPE_NAME);

        String name = computeDocumentName("xp-" + xpi.getId());
        String targetPath = new Path(containerPath).append(name).toString();
        boolean exist = false;
        if (session.exists(new PathRef(targetPath))) {
            exist = true;
            doc = session.getDocument(new PathRef(targetPath));
        }
        doc.setPathInfo(containerPath, name);
        doc.setPropertyValue("dc:title", xpi.getId());

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
    public Collection<ExtensionInfo> getExtensions() {
        List<ExtensionInfo> result = new ArrayList<ExtensionInfo>();
        try {
            // find root doc for distribution
            DocumentModel dist = doc;
            while (!DistributionSnapshot.TYPE_NAME.equals(dist.getType())) {
                dist = getCoreSession().getParentDocument(dist.getRef());
            }
            String query = QueryHelper.select(ExtensionInfo.TYPE_NAME, dist,
                    ExtensionInfo.PROP_EXTENSION_POINT, getId());
            DocumentModelList docs = getCoreSession().query(query);
            for (DocumentModel contribDoc : docs) {
                ExtensionInfo contrib = contribDoc.getAdapter(ExtensionInfo.class);
                if (contrib != null) {
                    result.add(contrib);
                }
            }
        } catch (Exception e) {
            log.error("Error while fetching contributions", e);
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
        } catch (Exception e) {
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
        return path.replace(toReplace, "/"
                + VirtualNodesConsts.ExtensionPoints_VNODE_NAME + "/" + getId());
    }

}
