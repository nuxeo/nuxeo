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

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.dom4j.DocumentException;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.VirtualNodesConsts;
import org.nuxeo.apidoc.documentation.ContributionItem;
import org.nuxeo.apidoc.documentation.DocumentationHelper;
import org.nuxeo.apidoc.documentation.XMLContributionParser;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.runtime.model.ComponentName;

public class ExtensionInfoDocAdapter extends BaseNuxeoArtifactDocAdapter implements ExtensionInfo {

    public static ExtensionInfoDocAdapter create(ExtensionInfo xi, int index, CoreSession session,
            String containerPath) {

        DocumentModel doc = session.createDocumentModel(TYPE_NAME);

        String id = xi.getId();
        if (index > 0) {
            id += "-" + index;
        }

        String name = computeDocumentName("contrib-" + id);
        String targetPath = new Path(containerPath).append(name).toString();
        boolean exist = false;
        if (session.exists(new PathRef(targetPath))) {
            exist = true;
            doc = session.getDocument(new PathRef(targetPath));
        }
        doc.setPathInfo(containerPath, name);
        doc.setPropertyValue(NuxeoArtifact.TITLE_PROPERTY_PATH, id);

        doc.setPropertyValue(PROP_CONTRIB_ID, id);
        doc.setPropertyValue(PROP_DOC, xi.getDocumentation());
        doc.setPropertyValue(PROP_EXTENSION_POINT, xi.getExtensionPoint());
        doc.setPropertyValue(PROP_TARGET_COMPONENT_NAME, xi.getTargetComponentName().getName());

        Blob xmlBlob = Blobs.createBlob(xi.getXml(), "text/xml", null, "contrib.xml"); // !!!!!
        doc.setPropertyValue(NuxeoArtifact.CONTENT_PROPERTY_PATH, (Serializable) xmlBlob);

        if (exist) {
            doc = session.saveDocument(doc);
        } else {
            doc = session.createDocument(doc);
        }

        return new ExtensionInfoDocAdapter(doc);
    }

    public ExtensionInfoDocAdapter(DocumentModel doc) {
        super(doc);
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
    public String getExtensionPoint() {
        return safeGet(PROP_EXTENSION_POINT);
    }

    @Override
    public String getId() {
        return safeGet(PROP_CONTRIB_ID);
    }

    @Override
    public ComponentName getTargetComponentName() {
        return new ComponentName(safeGet(PROP_TARGET_COMPONENT_NAME));
    }

    @Override
    public String getXml() {
        return safeGetContent(safeGet(NuxeoArtifact.CONTENT_PROPERTY_PATH), "");
    }

    @Override
    public String getVersion() {

        BundleInfo parentBundle = getParentNuxeoArtifact(BundleInfo.class);

        if (parentBundle != null) {
            return parentBundle.getVersion();
        }

        log.error("Unable to determine version for Contribution " + getId());
        return "?";
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public String getHierarchyPath() {
        String path = super.getHierarchyPath() + "###";
        String toReplace = "/" + getId() + "###";
        return path.replace(toReplace, "/" + VirtualNodesConsts.Contributions_VNODE_NAME + "/" + getId());
    }

    @Override
    public List<ContributionItem> getContributionItems() {
        try {
            return XMLContributionParser.extractContributionItems(getXml());
        } catch (DocumentException e) {
            log.error(e, e);
            return Collections.emptyList();
        }
    }

    @Override
    public ComponentInfo getComponent() {
        String cId = getId().split("--")[0];
        ComponentInfo parentComponent = getParentNuxeoArtifact(ComponentInfo.class);
        if (parentComponent.getId().equals(cId)) {
            return parentComponent;
        }
        return null;
    }

}
