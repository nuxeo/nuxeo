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

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.dom4j.DocumentException;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.VirtualNodesConsts;
import org.nuxeo.apidoc.documentation.ContributionItem;
import org.nuxeo.apidoc.documentation.DocumentationHelper;
import org.nuxeo.apidoc.documentation.XMLContributionParser;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.runtime.model.ComponentName;

public class ExtensionInfoDocAdapter extends BaseNuxeoArtifactDocAdapter implements ExtensionInfo {

    public static ExtensionInfoDocAdapter create(ExtensionInfo xi, CoreSession session, String containerPath)
            throws ClientException {

        DocumentModel doc = session.createDocumentModel(TYPE_NAME);

        String name = computeDocumentName("contrib-" + xi.getId());
        String targetPath = new Path(containerPath).append(name).toString();
        boolean exist = false;
        if (session.exists(new PathRef(targetPath))) {
            exist = true;
            doc = session.getDocument(new PathRef(targetPath));
        }
        doc.setPathInfo(containerPath, name);
        doc.setPropertyValue("dc:title", xi.getId());

        doc.setPropertyValue(PROP_CONTRIB_ID, xi.getId());
        doc.setPropertyValue(PROP_DOC, xi.getDocumentation());
        doc.setPropertyValue(PROP_EXTENSION_POINT, xi.getExtensionPoint());
        doc.setPropertyValue(PROP_TARGET_COMPONENT_NAME, xi.getTargetComponentName().getName());

        Blob xmlBlob = Blobs.createBlob(xi.getXml(), "text/xml", null, "contrib.xml"); // !!!!!
        doc.setPropertyValue("file:content", (Serializable) xmlBlob);

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
        try {
            Blob xml = safeGet(Blob.class, "file:content", null);
            if (xml == null) {
                return "";
            }
            if (xml.getEncoding() == null || "".equals(xml.getEncoding())) {
                xml.setEncoding("utf-8");
            }
            return xml.getString();
        } catch (IOException e) {
            log.error("Error while reading blob", e);
            return "";
        }
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
