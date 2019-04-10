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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.QueryHelper;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.documentation.DocumentationHelper;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;

public class ComponentInfoDocAdapter extends BaseNuxeoArtifactDocAdapter implements ComponentInfo {

    public ComponentInfoDocAdapter(DocumentModel doc) {
        super(doc);
    }

    public static ComponentInfoDocAdapter create(ComponentInfo componentInfo, CoreSession session, String containerPath)
            throws ClientException, IOException {

        DocumentModel doc = session.createDocumentModel(TYPE_NAME);

        String name = computeDocumentName("component-" + componentInfo.getId());
        String targetPath = new Path(containerPath).append(name).toString();
        boolean exist = false;
        if (session.exists(new PathRef(targetPath))) {
            exist = true;
            doc = session.getDocument(new PathRef(targetPath));
        }
        doc.setPathInfo(containerPath, name);
        doc.setPropertyValue("dc:title", componentInfo.getName());
        doc.setPropertyValue(PROP_COMPONENT_ID, componentInfo.getId());
        doc.setPropertyValue(PROP_COMPONENT_NAME, componentInfo.getName());
        doc.setPropertyValue(PROP_COMPONENT_CLASS, componentInfo.getComponentClass());
        doc.setPropertyValue(PROP_BUILT_IN_DOC, componentInfo.getDocumentation());
        doc.setPropertyValue(PROP_IS_XML, Boolean.valueOf(componentInfo.isXmlPureComponent()));
        doc.setPropertyValue(PROP_SERVICES, (Serializable) componentInfo.getServiceNames());

        Blob xmlBlob = Blobs.createBlob(componentInfo.getXmlFileContent(), "text/xml", null,
                componentInfo.getXmlFileName());
        doc.setPropertyValue("file:content", (Serializable) xmlBlob);

        if (exist) {
            doc = session.saveDocument(doc);
        } else {
            doc = session.createDocument(doc);
        }
        return new ComponentInfoDocAdapter(doc);
    }

    @Override
    public BundleInfo getBundle() {
        try {
            DocumentModel parent = getCoreSession().getDocument(doc.getParentRef());
            return parent.getAdapter(BundleInfo.class);
        } catch (ClientException e) {
            log.error(e, e);
        }
        return null;
    }

    @Override
    public String getComponentClass() {
        return safeGet(PROP_COMPONENT_CLASS);
    }

    @Override
    public String getDocumentation() {
        return safeGet(PROP_BUILT_IN_DOC);
    }

    @Override
    public String getDocumentationHtml() {
        return DocumentationHelper.getHtml(getDocumentation());
    }

    @Override
    public ExtensionPointInfo getExtensionPoint(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<ExtensionPointInfo> getExtensionPoints() {
        List<ExtensionPointInfo> xps = new ArrayList<ExtensionPointInfo>();
        try {
            String query = QueryHelper.select(ExtensionPointInfo.TYPE_NAME, doc);
            DocumentModelList docs = getCoreSession().query(query);
            for (DocumentModel child : docs) {
                ExtensionPointInfo xp = child.getAdapter(ExtensionPointInfo.class);
                if (xp != null) {
                    xps.add(xp);
                }
            }
        } catch (ClientException e) {
            log.error(e, e);
        }
        return xps;
    }

    @Override
    public Collection<ExtensionInfo> getExtensions() {
        List<ExtensionInfo> contribs = new ArrayList<ExtensionInfo>();
        try {
            String query = QueryHelper.select(ExtensionInfo.TYPE_NAME, doc);
            DocumentModelList docs = getCoreSession().query(query);
            for (DocumentModel child : docs) {
                ExtensionInfo xp = child.getAdapter(ExtensionInfo.class);
                if (xp != null) {
                    contribs.add(xp);
                }
            }
        } catch (ClientException e) {
            log.error(e, e);
        }
        return contribs;
    }

    @Override
    public String getName() {
        return safeGet(PROP_COMPONENT_NAME);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getServiceNames() {
        try {
            return (List<String>) doc.getPropertyValue(PROP_SERVICES);
        } catch (ClientException e) {
            log.error("Error while getting service names", e);
        }
        return null;
    }

    @Override
    public String getXmlFileContent() throws IOException {
        try {
            Blob xml = safeGet(Blob.class, "file:content", null);
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
    public String getXmlFileName() {
        Blob xml = safeGet(Blob.class, "file:content", null);
        return xml == null ? "" : xml.getFilename() == null ? "" : xml.getFilename();
    }

    @Override
    public URL getXmlFileUrl() {
        return null;
    }

    @Override
    public boolean isXmlPureComponent() {
        Boolean isXml = safeGet(Boolean.class, PROP_IS_XML, Boolean.TRUE);
        return isXml == null ? true : isXml.booleanValue();
    }

    @Override
    public String getId() {
        return getName();
    }

    @Override
    public String getVersion() {

        BundleInfo parentBundle = getParentNuxeoArtifact(BundleInfo.class);

        if (parentBundle != null) {
            return parentBundle.getVersion();
        }

        log.error("Unable to determine version for Component " + getId());
        return "?";
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public List<ServiceInfo> getServices() {
        List<ServiceInfo> result = new ArrayList<ServiceInfo>();
        try {
            String query = QueryHelper.select(ServiceInfo.TYPE_NAME, doc);
            DocumentModelList docs = getCoreSession().query(query);
            for (DocumentModel siDoc : docs) {
                ServiceInfo si = siDoc.getAdapter(ServiceInfo.class);
                if (si != null) {
                    result.add(si);
                }
            }
        } catch (ClientException e) {
            log.error("Unable to fetch NXService", e);
        }
        return result;
    }

}
