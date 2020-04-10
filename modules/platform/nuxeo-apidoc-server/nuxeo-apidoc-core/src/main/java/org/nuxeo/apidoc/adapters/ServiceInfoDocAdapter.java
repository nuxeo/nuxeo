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

import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.api.VirtualNodesConsts;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;

public class ServiceInfoDocAdapter extends BaseNuxeoArtifactDocAdapter implements ServiceInfo {

    public ServiceInfoDocAdapter(DocumentModel doc) {
        super(doc);
    }

    public static ServiceInfoDocAdapter create(ServiceInfo si, CoreSession session, String containerPath) {

        DocumentModel doc = session.createDocumentModel(TYPE_NAME);

        String name = computeDocumentName("service-" + si.getId());
        String targetPath = new Path(containerPath).append(name).toString();
        boolean exist = false;
        if (session.exists(new PathRef(targetPath))) {
            exist = true;
            doc = session.getDocument(new PathRef(targetPath));
        }
        doc.setPathInfo(containerPath, name);
        doc.setPropertyValue(NuxeoArtifact.TITLE_PROPERTY_PATH, si.getId());

        doc.setPropertyValue(PROP_CLASS_NAME, si.getId());
        doc.setPropertyValue(PROP_COMPONENT_ID, si.getComponentId());
        doc.setPropertyValue(PROP_OVERRIDEN, si.isOverriden());

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
    public boolean isOverriden() {
        return safeGet(Boolean.class, PROP_OVERRIDEN, false);
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
    public ComponentInfo getComponent() {
        return getParentNuxeoArtifact(ComponentInfo.class);
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
