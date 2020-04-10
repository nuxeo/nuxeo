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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.documentation.ResourceDocumentationItem;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;

public class BundleInfoDocAdapter extends BaseNuxeoArtifactDocAdapter implements BundleInfo {

    public static BundleInfoDocAdapter create(BundleInfo bundleInfo, CoreSession session, String containerPath) {

        DocumentModel doc = session.createDocumentModel(TYPE_NAME);
        String name = computeDocumentName("bundle-" + bundleInfo.getId());
        String targetPath = new Path(containerPath).append(name).toString();
        boolean exist = false;
        if (session.exists(new PathRef(targetPath))) {
            exist = true;
            doc = session.getDocument(new PathRef(targetPath));
        }
        doc.setPathInfo(containerPath, name);
        doc.setPropertyValue(NuxeoArtifact.TITLE_PROPERTY_PATH, bundleInfo.getBundleId());
        doc.setPropertyValue(PROP_ARTIFACT_GROUP_ID, bundleInfo.getGroupId());
        doc.setPropertyValue(PROP_ARTIFACT_ID, bundleInfo.getArtifactId());
        doc.setPropertyValue(PROP_ARTIFACT_VERSION, bundleInfo.getArtifactVersion());
        doc.setPropertyValue(PROP_BUNDLE_ID, bundleInfo.getId());
        doc.setPropertyValue(PROP_JAR_NAME, bundleInfo.getFileName());
        String manifest = bundleInfo.getManifest();
        if (manifest != null) {
            Blob manifestBlob = Blobs.createBlob(manifest);
            manifestBlob.setFilename("MANIFEST.MF");
            doc.setPropertyValue(NuxeoArtifact.CONTENT_PROPERTY_PATH, (Serializable) manifestBlob);
        }

        if (exist) {
            doc = session.saveDocument(doc);
        } else {
            doc = session.createDocument(doc);
        }

        return new BundleInfoDocAdapter(doc);
    }

    public BundleInfoDocAdapter(DocumentModel doc) {
        super(doc);
    }

    @Override
    public String getArtifactId() {
        return safeGet(PROP_ARTIFACT_ID);
    }

    @Override
    public String getBundleId() {
        return safeGet(PROP_BUNDLE_ID);
    }

    @Override
    public List<ComponentInfo> getComponents() {
        List<ComponentInfo> components = new ArrayList<>();
        List<DocumentModel> children = getCoreSession().getChildren(doc.getRef());
        for (DocumentModel child : children) {
            ComponentInfo comp = child.getAdapter(ComponentInfo.class);
            if (comp != null) {
                components.add(comp);
            }
        }
        return components;
    }

    @Override
    public String getFileName() {
        return safeGet(PROP_JAR_NAME);
    }

    @Override
    public String getGroupId() {
        return safeGet(PROP_ARTIFACT_GROUP_ID);
    }

    @Override
    public String getLocation() {
        return null;
    }

    @Override
    public String getManifest() {
        try {
            Blob mf = safeGet(Blob.class, NuxeoArtifact.CONTENT_PROPERTY_PATH, null);
            if (mf == null) {
                return "No MANIFEST.MF";
            }
            if (mf.getEncoding() == null || "".equals(mf.getEncoding())) {
                mf.setEncoding("utf-8");
            }
            return mf.getString();
        } catch (IOException e) {
            log.error("Error while reading blob", e);
            return "";
        }
    }

    @Override
    public String[] getRequirements() {
        return null;
    }

    @Override
    public String getArtifactVersion() {
        return safeGet(PROP_ARTIFACT_VERSION, null);
    }

    @Override
    public String getId() {
        return getBundleId();
    }

    @Override
    public String getVersion() {
        return getArtifactVersion();
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public Map<String, ResourceDocumentationItem> getLiveDoc() {
        return null;
    }

    @Override
    public Map<String, ResourceDocumentationItem> getParentLiveDoc() {
        return null;
    }

}
