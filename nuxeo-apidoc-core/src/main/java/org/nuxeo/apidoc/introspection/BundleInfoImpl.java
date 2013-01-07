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
 *     Bogdan Stefanescu
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.introspection;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.documentation.AssociatedDocumentsImpl;
import org.nuxeo.apidoc.documentation.DocumentationHelper;
import org.nuxeo.apidoc.documentation.ResourceDocumentationItem;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.ExtensionPoint;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.w3c.dom.Document;

public class BundleInfoImpl extends BaseNuxeoArtifact implements BundleInfo {

    protected static final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

    protected static final XPathFactory xpathFactory = XPathFactory.newInstance();

    protected final String bundleId;

    protected final Collection<ComponentInfo> components;

    protected String fileName;

    protected String manifest; // TODO

    protected String[] requirements;

    protected String groupId;

    protected String artifactId;

    protected String artifactVersion;

    protected BundleGroup bundleGroup;

    protected Map<String, ResourceDocumentationItem> liveDoc;

    protected Map<String, ResourceDocumentationItem> parentLiveDoc;

    protected String location;

    private static final Log log = LogFactory.getLog(BundleInfoImpl.class);

    protected File jarFile;

    public BundleInfoImpl(String bundleId) {
        this.bundleId = bundleId;
        components = new ArrayList<ComponentInfo>();
    }

    public BundleInfoImpl(String bundleId, File jar) {
        this(bundleId);
        read(jar);
    }

    public void read(File jarFile) {

        setFileName(jarFile.getName());
        setLocation(jarFile.getAbsolutePath());

        try {
            if (jarFile.isDirectory()) {
                // directory: run from Eclipse in unit tests
                // .../nuxeo-runtime/nuxeo-runtime/bin
                // or sometimes
                // .../nuxeo-runtime/nuxeo-runtime/bin/main
                File manifest = new File(jarFile,
                        ServerInfo.META_INF_MANIFEST_MF);
                if (manifest.exists()) {
                    InputStream is = new FileInputStream(manifest);
                    String mf = FileUtils.read(is);
                    setManifest(mf);
                }
                // find and parse pom.xml
                File up = new File(jarFile, "..");
                File pom = new File(up, ServerInfo.POM_XML);
                if (!pom.exists()) {
                    pom = new File(new File(up, ".."), ServerInfo.POM_XML);
                    if (!pom.exists()) {
                        pom = null;
                    }
                }
                if (pom != null) {
                    DocumentBuilder b = documentBuilderFactory.newDocumentBuilder();
                    Document doc = b.parse(new FileInputStream(pom));
                    XPath xpath = xpathFactory.newXPath();
                    String groupId = (String) xpath.evaluate(
                            "//project/groupId", doc, XPathConstants.STRING);
                    if ("".equals(groupId)) {
                        groupId = (String) xpath.evaluate(
                                "//project/parent/groupId", doc,
                                XPathConstants.STRING);
                    }
                    String artifactId = (String) xpath.evaluate(
                            "//project/artifactId", doc, XPathConstants.STRING);
                    if ("".equals(artifactId)) {
                        artifactId = (String) xpath.evaluate(
                                "//project/parent/artifactId", doc,
                                XPathConstants.STRING);
                    }
                    String version = (String) xpath.evaluate(
                            "//project/version", doc, XPathConstants.STRING);
                    if ("".equals(version)) {
                        version = (String) xpath.evaluate(
                                "//project/parent/version", doc,
                                XPathConstants.STRING);
                    }
                    setArtifactId(artifactId);
                    setGroupId(groupId);
                    setArtifactVersion(version);
                }
            } else {
                ZipFile zFile = new ZipFile(jarFile);
                this.jarFile = jarFile;
                ZipEntry mfEntry = zFile.getEntry(ServerInfo.META_INF_MANIFEST_MF);
                if (mfEntry != null) {
                    InputStream mfStream = zFile.getInputStream(mfEntry);
                    String mf = FileUtils.read(mfStream);
                    setManifest(mf);
                }
                Enumeration<? extends ZipEntry> entries = zFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.getName().endsWith(ServerInfo.POM_PROPERTIES)) {
                        InputStream is = zFile.getInputStream(entry);
                        PropertyResourceBundle prb = new PropertyResourceBundle(
                                is);
                        String groupId = prb.getString("groupId");
                        String artifactId = prb.getString("artifactId");
                        String version = prb.getString("version");
                        setArtifactId(artifactId);
                        setGroupId(groupId);
                        setArtifactVersion(version);
                        is.close();
                        break;
                    }
                }
                zFile.close();
                zFile = new ZipFile(jarFile);
                EmbeddedDocExtractor.extractEmbeddedDoc(zFile, this);
                zFile.close();
            }
        } catch (Exception e) {
            log.error(e, e);
        }

    }

    public BundleGroup getBundleGroup() {
        return bundleGroup;
    }

    public void setBundleGroup(BundleGroup bundleGroup) {
        this.bundleGroup = bundleGroup;
    }

    @Override
    public Collection<ComponentInfo> getComponents() {
        return components;
    }

    public void addComponent(ComponentInfoImpl component) {
        components.add(component);
    }

    public void addComponent(RegistrationInfo ri) {
        addComponent(ri, null, null, null);
    }

    public void addComponent(RegistrationInfo ri, ServerInfo server,
            Map<String, ExtensionPointInfoImpl> xpRegistry,
            List<ExtensionInfoImpl> contribRegistry) {
        ComponentInfoImpl component = new ComponentInfoImpl(this,
                ri.getName().getName());

        if (ri.getExtensionPoints() != null) {
            for (ExtensionPoint xp : ri.getExtensionPoints()) {
                ExtensionPointInfoImpl xpinfo = new ExtensionPointInfoImpl(
                        component, xp.getName());
                Class<?>[] ctypes = xp.getContributions();
                String[] descriptors = new String[ctypes.length];

                for (int i = 0; i < ctypes.length; i++) {
                    descriptors[i] = ctypes[i].getCanonicalName();
                    List<Class<?>> spi = SPI.filter(ctypes[i]);
                    xpinfo.addSpi(spi);
                    if (server != null) {
                        server.allSpi.addAll(spi);
                    }
                }
                xpinfo.setDescriptors(descriptors);
                xpinfo.setDocumentation(xp.getDocumentation());
                if (xpRegistry != null) {
                    xpRegistry.put(xpinfo.getId(), xpinfo);
                }
                component.addExtensionPoint(xpinfo);
            }
        }

        component.setXmlFileUrl(ri.getXmlFileUrl());

        if (ri.getProvidedServiceNames() != null) {
            for (String serviceName : ri.getProvidedServiceNames()) {
                component.addService(serviceName);
            }
        }

        if (ri.getExtensions() != null) {
            for (Extension xt : ri.getExtensions()) {
                ExtensionInfoImpl xtinfo = new ExtensionInfoImpl(component,
                        xt.getExtensionPoint());
                xtinfo.setTargetComponentName(xt.getTargetComponent());
                xtinfo.setContribution(xt.getContributions());
                xtinfo.setDocumentation(xt.getDocumentation());
                xtinfo.setXml(DocumentationHelper.secureXML(xt.toXML()));

                if (contribRegistry != null) {
                    contribRegistry.add(xtinfo);
                }
                component.addExtension(xtinfo);
            }
        }
        component.setComponentClass(ri.getImplementation());
        component.setDocumentation(ri.getDocumentation());
        addComponent(component);
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String getBundleId() {
        return bundleId;
    }

    @Override
    public String[] getRequirements() {
        return requirements;
    }

    public void setRequirements(String[] requirements) {
        this.requirements = requirements;
    }

    @Override
    public String getManifest() {
        return manifest;
    }

    public void setManifest(String manifest) {
        this.manifest = manifest;
    }

    @Override
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public String getArtifactGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    @Override
    public String getArtifactVersion() {
        return artifactVersion;
    }

    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    @Override
    public String getId() {
        return bundleId;
    }

    @Override
    public String getVersion() {
        return artifactVersion;
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public String getHierarchyPath() {
        return bundleGroup.getHierarchyPath() + "/" + getId();
    }

    public void setLiveDoc(Map<String, ResourceDocumentationItem> liveDoc) {
        this.liveDoc = liveDoc;
    }

    public void setParentLiveDoc(
            Map<String, ResourceDocumentationItem> parentLiveDoc) {
        this.parentLiveDoc = parentLiveDoc;
    }

    protected Map<String, ResourceDocumentationItem> getMergedDocumentation() {

        Map<String, ResourceDocumentationItem> merged = parentLiveDoc;
        if (merged == null) {
            merged = new HashMap<String, ResourceDocumentationItem>();
        }
        if (liveDoc != null) {
            for (String key : liveDoc.keySet()) {
                if (liveDoc.get(key) != null) {
                    merged.put(key, liveDoc.get(key));
                }
            }
        }
        return merged;
    }

    @Override
    public AssociatedDocumentsImpl getAssociatedDocuments(CoreSession session) {
        AssociatedDocumentsImpl docs = super.getAssociatedDocuments(session);
        docs.setLiveDoc(getMergedDocumentation());
        return docs;
    }

    public Map<String, ResourceDocumentationItem> getLiveDoc() {
        return liveDoc;
    }

    public Map<String, ResourceDocumentationItem> getParentLiveDoc() {
        return parentLiveDoc;
    }

    public Blob getResource(String resPath) throws IOException {
        if (jarFile == null) {
            return null;
        }
        ZipFile zip = new ZipFile(jarFile);
        try {
            ZipEntry mfEntry = zip.getEntry(resPath);
            if (mfEntry != null) {
                InputStream stream = zip.getInputStream(mfEntry);
                return new ByteArrayBlob(FileUtils.readBytes(stream));
            }
        } finally {
            zip.close();
        }
        return null;
    }
}
