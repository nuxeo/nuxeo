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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;

/**
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
public class ComponentInfoDocAdapter extends BaseNuxeoArtifactDocAdapter implements ComponentInfo {

    public static ComponentInfoDocAdapter create(ComponentInfo componentInfo, CoreSession session, String containerPath) throws Exception {

        DocumentModel doc = session.createDocumentModel(TYPE_NAME);

        String name = computeDocumentName(componentInfo.getName());
        String targetPath = new Path(containerPath).append(name).toString();
        boolean exist = false;
        if (session.exists(new PathRef(targetPath))) {
            exist = true;
            doc = session.getDocument(new PathRef(targetPath));
        }
        doc.setPathInfo(containerPath, name);
        doc.setPropertyValue("dc:title", componentInfo.getName());
        doc.setPropertyValue("nxcomponent:componentId", componentInfo.getId());
        doc.setPropertyValue("nxcomponent:componentName", componentInfo.getName());
        doc.setPropertyValue("nxcomponent:componentClass", componentInfo.getComponentClass());
        doc.setPropertyValue("nxcomponent:builtInDocumentation", componentInfo.getDocumentation());
        doc.setPropertyValue("nxcomponent:isXML", componentInfo.isXmlPureComponent());
        doc.setPropertyValue("nxcomponent:services", (Serializable) componentInfo.getServiceNames());

        Blob xmlBlob = new StringBlob(componentInfo.getXmlFileContent());
        String xmlFileName ="descriptor.xml";
        if (componentInfo.getXmlFileUrl()!=null) {
            xmlFileName = new Path(componentInfo.getXmlFileUrl().getFile()).lastSegment();
        }
        xmlBlob.setFilename(xmlFileName);
        xmlBlob.setMimeType("text/xml");
        doc.setPropertyValue("file:content",(Serializable) xmlBlob);

        if (exist) {
            doc = session.saveDocument(doc);
        } else {
            doc = session.createDocument(doc);
        }
        return new ComponentInfoDocAdapter(doc);
    }


    public ComponentInfoDocAdapter(DocumentModel doc) {
        super(doc);
    }

    public BundleInfo getBundle() {
        try {
            DocumentModel parent = getCoreSession().getDocument(doc.getParentRef());
            return parent.getAdapter(BundleInfo.class);
        }
        catch (Exception e) {
            // TODO: handle exception
        }
        return null;
    }

    public String getComponentClass() {
        return safeGet("nxcomponent:componentClass");
    }

    public String getDocumentation() {
        return safeGet("nxcomponent:builtInDocumentation");
    }

    public ExtensionPointInfo getExtensionPoint(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<ExtensionPointInfo> getExtensionPoints() {
        List<ExtensionPointInfo> xps = new ArrayList<ExtensionPointInfo>();
        try {
            String query = "select * from NXExtensionPoint where ecm:path STARTSWITH '" + doc.getPathAsString() + "'";

            DocumentModelList docs = getCoreSession().query(query);
            for(DocumentModel child : docs) {
                ExtensionPointInfo xp = child.getAdapter(ExtensionPointInfo.class);
                if (xp!=null) {
                    xps.add(xp);
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
        return xps;
    }

    public Collection<ExtensionInfo> getExtensions() {
        List<ExtensionInfo> contribs = new ArrayList<ExtensionInfo>();
        try {
            String query = "select * from NXContribution where ecm:path STARTSWITH '" + doc.getPathAsString() + "'";

            DocumentModelList docs = getCoreSession().query(query);
            for(DocumentModel child : docs) {
                ExtensionInfo xp = child.getAdapter(ExtensionInfo.class);
                if (xp!=null) {
                    contribs.add(xp);
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
        return contribs;
    }

    public String getName() {
        return safeGet("nxcomponent:componentName");
    }

    public List<String> getServiceNames() {
        try {
            return (List<String>) doc.getPropertyValue("nxcomponent:services");
        }
        catch (Exception e) {
            log.error("Error while getting service names", e);
        }
        return null;
    }

    public String getXmlFileContent() throws IOException {
        try {
            Blob xml = safeGet(Blob.class, "file:content", null);
            return xml.getString();
        } catch (IOException e) {
            log.error("Error while reading blob", e);
            return "";
        }
    }

    public URL getXmlFileUrl() {
        return null;
    }

    public boolean isXmlPureComponent() {
        return safeGet(Boolean.class,"nxcomponent:isXML", new Boolean(true));
    }

    @Override
    public String getId() {
        return getName();
    }

    public String getVersion() {

        BundleInfo parentBundle = getParentNuxeoArtifact(BundleInfo.class);

        if (parentBundle!=null) {
            return parentBundle.getVersion();
        }

        log.error("Unable to determine version for Component " + getId());
        return "?";
    }


    public String getArtifactType() {
        return ComponentInfo.TYPE_NAME;
    }

}
