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

import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.api.VirtualNodesConsts;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;

public class ServiceInfoDocAdapter extends BaseNuxeoArtifactDocAdapter implements ServiceInfo {

    public ServiceInfoDocAdapter(DocumentModel doc) {
        super(doc);
    }

    public static ServiceInfoDocAdapter create(ServiceInfo si, CoreSession session, String containerPath)
            throws ClientException {

        DocumentModel doc = session.createDocumentModel(TYPE_NAME);

        String name = computeDocumentName("service-" + si.getId());
        String targetPath = new Path(containerPath).append(name).toString();
        boolean exist = false;
        if (session.exists(new PathRef(targetPath))) {
            exist = true;
            doc = session.getDocument(new PathRef(targetPath));
        }
        doc.setPathInfo(containerPath, name);
        doc.setPropertyValue("dc:title", si.getId());

        doc.setPropertyValue(PROP_CLASS_NAME, si.getId());
        doc.setPropertyValue(PROP_COMPONENT_ID, si.getComponentId());

        if (exist) {
            doc = session.saveDocument(doc);
        } else {
            doc = session.createDocument(doc);
        }

        return new ServiceInfoDocAdapter(doc);
    }

    @Override
    public String getId() {
        return safeGet(PROP_CLASS_NAME, "unknown_service");
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public String getVersion() {
        BundleInfo parentBundle = getParentNuxeoArtifact(BundleInfo.class);

        if (parentBundle != null) {
            return parentBundle.getVersion();
        }

        log.error("Unable to determine version for Service " + getId());
        return "?";
    }

    @Override
    public String getComponentId() {
        return safeGet(PROP_COMPONENT_ID, "unknown_service");
    }

    @Override
    public String getHierarchyPath() {
        String path = super.getHierarchyPath() + "###";
        String toReplace = "/" + getId() + "###";
        return path.replace(toReplace, "/" + VirtualNodesConsts.Services_VNODE_NAME + "/" + getId());
    }
}
