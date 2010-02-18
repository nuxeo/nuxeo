/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.apidoc.adapters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;

/**
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
public class BundleInfoDocAdapter extends BaseNuxeoArtifactDocAdapter implements
        BundleInfo {

    public static BundleInfoDocAdapter create(BundleInfo bundleInfo, CoreSession session, String containerPath) throws ClientException {

        DocumentModel doc = session.createDocumentModel(TYPE_NAME);
        String name = computeDocumentName(bundleInfo.getBundleId());
        String targetPath = new Path(containerPath).append(name).toString();
        boolean exist = false;
        if (session.exists(new PathRef(targetPath))) {
            exist = true;
            doc = session.getDocument(new PathRef(targetPath));
        }
        doc.setPathInfo(containerPath, name);
        doc.setPropertyValue("dc:title", bundleInfo.getBundleId());
        doc.setPropertyValue("nxbundle:artifactGroupId", bundleInfo.getArtifactGroupId());
        doc.setPropertyValue("nxbundle:artifactId", bundleInfo.getArtifactId());
        doc.setPropertyValue("nxbundle:artifactVersion", bundleInfo.getArtifactVersion());
        doc.setPropertyValue("nxbundle:bundleId", bundleInfo.getBundleId());
        doc.setPropertyValue("nxbundle:jarName", bundleInfo.getFileName());
        Blob manifestBlob = new StringBlob(bundleInfo.getManifest());
        manifestBlob.setFilename("MANIFEST.MF");
        manifestBlob.setMimeType("text/plain");
        doc.setPropertyValue("file:content",(Serializable) manifestBlob);

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

    public String getArtifactId() {
        try {
            return (String) doc.getPropertyValue("nxbundle:artifactId");
        } catch (Exception e) {
            return null;
        }
    }

    public String getBundleId() {
        try {
            return (String) doc.getPropertyValue("nxbundle:bundleId");
        } catch (Exception e) {
            return null;
        }
    }

    public Collection<ComponentInfo> getComponents() {
        List<ComponentInfo> components = new ArrayList<ComponentInfo>();

        try {
            List<DocumentModel> children = getCoreSession().getChildren(doc.getRef());

            for (DocumentModel child : children) {
                ComponentInfo comp = child.getAdapter(ComponentInfo.class);
                if (comp!=null) {
                    components.add(comp);
                }
            }
        }
        catch (Exception e) {
            // TODO: handle exception
        }
        return components;
    }

    public String getFileName() {
        try {
            return (String) doc.getPropertyValue("nxbundle:jarName");
        } catch (Exception e) {
            return null;
        }
    }

    public String getArtifactGroupId() {
        try {
            return (String) doc.getPropertyValue("nxbundle:artifactGroupId");
        } catch (Exception e) {
            return null;
        }
    }

    public String getLocation() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getManifest() {
        try {
            Blob mf = (Blob) doc.getPropertyValue("file:content");
            return mf.getString();
        } catch (Exception e) {
            return null;
        }
    }

    public String[] getRequirements() {
        return null;
    }

    public String getArtifactVersion() {
        try {
            return (String) doc.getPropertyValue("nxbundle:artifactVersion");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getId() {
        return getBundleId();
    }

    public String getVersion() {
        return getArtifactVersion();
    }

    public String getArtifactType() {
        return BundleInfo.TYPE_NAME;
    }
}
