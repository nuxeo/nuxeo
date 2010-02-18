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

import java.io.IOException;
import java.io.Serializable;

import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.runtime.model.ComponentName;

/**
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
public class ExtensionInfoDocAdapter extends BaseNuxeoArtifactDocAdapter
        implements ExtensionInfo {

    public static ExtensionInfoDocAdapter create(ExtensionInfo xi,
            CoreSession session, String containerPath) throws Exception {

        DocumentModel doc = session.createDocumentModel(TYPE_NAME);

        String name = computeDocumentName(xi.getId());
        String targetPath = new Path(containerPath).append(name).toString();
        boolean exist = false;
        if (session.exists(new PathRef(targetPath))) {
            exist = true;
            doc = session.getDocument(new PathRef(targetPath));
        }
        doc.setPathInfo(containerPath, name);
        doc.setPropertyValue("dc:title", xi.getId());

        doc.setPropertyValue("nxcontribution:contribId", xi.getId());
        doc.setPropertyValue("nxcontribution:documentation", xi
                .getDocumentation());
        doc.setPropertyValue("nxcontribution:extensionPoint", xi
                .getExtensionPoint());
        doc.setPropertyValue("nxcontribution:targetComponentName", xi
                .getTargetComponentName().getName());

        Blob xmlBlob = new StringBlob(xi.getXml());
        xmlBlob.setFilename("contrib.xml"); // !!!!!
        xmlBlob.setMimeType("text/xml");
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

    public String getDocumentation() {
        return safeGet("nxcontribution:documentation");
    }

    public String getExtensionPoint() {
        return safeGet("nxcontribution:extensionPoint");
    }

    public String getId() {
        return safeGet("nxcontribution:contribId");
    }

    public ComponentName getTargetComponentName() {
        return new ComponentName(safeGet("nxcontribution:targetComponentName"));
    }

    public String getXml() {
        try {
            Blob xml = safeGet(Blob.class, "file:content", new StringBlob(""));
            return xml.getString();
        } catch (IOException e) {
            log.error("Error while reading blob", e);
            return "";
        }
    }

    public String getVersion() {

        BundleInfo parentBundle = getParentNuxeoArtifact(BundleInfo.class);

        if (parentBundle!=null) {
            return parentBundle.getVersion();
        }

        log.error("Unable to determine version for Contribution " + getId());
        return "?";
    }

    public String getArtifactType() {
        return ExtensionInfo.TYPE_NAME;
    }

}
