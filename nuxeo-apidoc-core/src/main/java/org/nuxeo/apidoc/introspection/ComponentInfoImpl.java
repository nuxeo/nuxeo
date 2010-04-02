/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.apidoc.introspection;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.api.BaseNuxeoArtifact;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.Path;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
public class ComponentInfoImpl extends BaseNuxeoArtifact implements ComponentInfo {

    protected BundleInfoImpl bundle;
    protected String name;
    protected Map<String, ExtensionPointInfo> extensionPoints;
    protected Collection<ExtensionInfo> extensions;
    protected List<String> serviceNames = new ArrayList<String>();
    protected List<ServiceInfo> services = new ArrayList<ServiceInfo>();

    protected URL xmlFileUrl;

    protected String componentClass =null;

    protected String documentation; //TODO

    protected static Log log = LogFactory.getLog(ComponentInfoImpl.class);

    public ComponentInfoImpl(BundleInfoImpl binfo, String name) {
        this.bundle = binfo;
        this.name = name;
        extensionPoints = new HashMap<String, ExtensionPointInfo>();
        extensions = new ArrayList<ExtensionInfo>();
    }

    public String getName() {
        return name;
    }

    public BundleInfoImpl getBundle() {
        return bundle;
    }

    public Collection<ExtensionPointInfo> getExtensionPoints() {
        return extensionPoints.values();
    }

    public Collection<ExtensionInfo> getExtensions() {
        return extensions;
    }

    public void addExtensionPoint(ExtensionPointInfoImpl xp) {
        extensionPoints.put(xp.getId(), xp);
    }

    public ExtensionPointInfo getExtensionPoint(String name) {
        return extensionPoints.get(name);
    }

    public void addExtension(ExtensionInfoImpl xt) {
        extensions.add(xt);
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public void addService(String serviceName) {
        this.serviceNames.add(serviceName);
        ServiceInfo si = new ServiceInfoImpl(serviceName, this);
        this.services.add(si);
    }

    public List<String> getServiceNames() {
        return serviceNames;
    }

    public String getComponentClass() {
        return componentClass;
    }

    public void setComponentClass(String componentClass) {
        this.componentClass = componentClass;
    }


    public boolean isXmlPureComponent() {
        return componentClass==null;
    }

    public URL getXmlFileUrl() {
        return xmlFileUrl;
    }

    public void setXmlFileUrl(URL xmlFileUrl) {
        this.xmlFileUrl = xmlFileUrl;
    }

    public String getXmlFileName() {

        if (xmlFileUrl==null) {
            return "";
        }
        String path = xmlFileUrl.getPath();
        String[] parts = path.split("!");
        if (parts.length==2) {
            return parts[1];
        } else {
            return path;
        }
    }

    public String getXmlFileContent() {

        if (xmlFileUrl==null) {
            return "";
        }
        String path = xmlFileUrl.getPath();
        String[] parts = path.split("!");

        File jar = new File(parts[0].replace("file:", ""));
        if (!jar.exists()) {
            return "Unable to locate Bundle :" +parts[0];
        }

        try {
            if (jar.getAbsolutePath().endsWith(".xml")) {
                return FileUtils.read(new FileInputStream(jar));
            }

            if (jar.isDirectory()) {
                File xml = new File(new Path(jar.getAbsolutePath()).append(parts[1]).toString());
                if (!xml.exists()) {
                    return "Unable to locate file :" + xml.getAbsolutePath();
                }
                return FileUtils.readFile(xml);
            } else {
                ZipFile jarArchive = new ZipFile(jar);
                ZipEntry entry  = jarArchive.getEntry(parts[1].substring(1));
                return FileUtils.read(jarArchive.getInputStream(entry));
            }
        }
        catch (Exception e) {
            log.error("Error while getting XML file", e);
            return "";
        }
    }

    @Override
    public String getId() {
        return getName();
    }

    public String getVersion() {
        return bundle.getVersion();
    }

    public String getArtifactType() {
        return ComponentInfo.TYPE_NAME;
    }

    public List<ServiceInfo> getServices() {
        return services;
    }
}
