/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.introspection;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.documentation.DocumentationHelper;
import org.nuxeo.common.utils.Path;

public class ComponentInfoImpl extends BaseNuxeoArtifact implements ComponentInfo {

    protected final BundleInfo bundle;

    protected final String name;

    protected final Map<String, ExtensionPointInfo> extensionPoints;

    protected final Collection<ExtensionInfo> extensions;

    protected final List<String> serviceNames = new ArrayList<>();

    protected final List<ServiceInfo> services = new ArrayList<>();

    protected URL xmlFileUrl;

    protected String componentClass;

    protected String documentation; // TODO

    protected static final Log log = LogFactory.getLog(ComponentInfoImpl.class);

    public ComponentInfoImpl(BundleInfo bundleInfo, String name) {
        bundle = bundleInfo;
        this.name = name;
        extensionPoints = new HashMap<>();
        extensions = new ArrayList<>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public BundleInfo getBundle() {
        return bundle;
    }

    @Override
    public Collection<ExtensionPointInfo> getExtensionPoints() {
        return extensionPoints.values();
    }

    @Override
    public Collection<ExtensionInfo> getExtensions() {
        return extensions;
    }

    public void addExtensionPoint(ExtensionPointInfoImpl xp) {
        extensionPoints.put(xp.getId(), xp);
    }

    @Override
    public ExtensionPointInfo getExtensionPoint(String name) {
        return extensionPoints.get(name);
    }

    public void addExtension(ExtensionInfoImpl xt) {
        extensions.add(xt);
    }

    @Override
    public String getDocumentation() {
        return documentation;
    }

    @Override
    public String getDocumentationHtml() {
        return DocumentationHelper.getHtml(getDocumentation());
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public void addService(String serviceName, boolean overriden) {
        serviceNames.add(serviceName);
        ServiceInfo si = new ServiceInfoImpl(serviceName, overriden, this);
        services.add(si);
    }

    @Override
    public List<String> getServiceNames() {
        return serviceNames;
    }

    @Override
    public String getComponentClass() {
        return componentClass;
    }

    public void setComponentClass(String componentClass) {
        this.componentClass = componentClass;
    }

    @Override
    public boolean isXmlPureComponent() {
        return componentClass == null;
    }

    @Override
    public URL getXmlFileUrl() {
        return xmlFileUrl;
    }

    public void setXmlFileUrl(URL xmlFileUrl) {
        this.xmlFileUrl = xmlFileUrl;
    }

    @Override
    public String getXmlFileName() {
        if (xmlFileUrl == null) {
            return "";
        }
        String path = xmlFileUrl.getPath();
        String[] parts = path.split("!");
        if (parts.length == 2) {
            return parts[1];
        } else {
            return path;
        }
    }

    @Override
    public String getXmlFileContent() {
        if (xmlFileUrl == null) {
            return "";
        }
        String path = xmlFileUrl.getPath();
        String[] parts = path.split("!");

        File jar = new File(parts[0].replace("file:", ""));
        if (!jar.exists()) {
            return "Unable to locate Bundle :" + parts[0];
        }

        try {
            String xml;
            if (jar.getAbsolutePath().endsWith(".xml")) {
                try (InputStream in = new FileInputStream(jar)) {
                    xml = IOUtils.toString(in, StandardCharsets.UTF_8);
                }
            } else if (jar.isDirectory()) {
                File file = new File(new Path(jar.getAbsolutePath()).append(parts[1]).toString());
                if (!file.exists()) {
                    return "Unable to locate file :" + file.getAbsolutePath();
                }
                xml = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            } else {
                try (ZipFile jarArchive = new ZipFile(jar)) {
                    ZipEntry entry = jarArchive.getEntry(parts[1].substring(1));
                    xml = IOUtils.toString(jarArchive.getInputStream(entry), StandardCharsets.UTF_8);
                }
            }
            return DocumentationHelper.secureXML(xml);
        } catch (IOException e) {
            log.error("Error while getting XML file", e);
            return "";
        }
    }

    @Override
    public String getId() {
        return name;
    }

    @Override
    public String getVersion() {
        return bundle.getVersion();
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public List<ServiceInfo> getServices() {
        return services;
    }

    @Override
    public String getHierarchyPath() {
        return getBundle().getHierarchyPath() + "/" + getId();
    }

}
