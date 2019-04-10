/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.documentation.DocumentationHelper;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.osgi.BundleImpl;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.ExtensionPoint;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.osgi.framework.Bundle;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The entry point to the server runtime introspection To build a description of
 * the current running server you need to create a {@link ServerInfo} object
 * using the method {@link #build(String, String)}.
 * <p>
 * Example
 *
 * <pre>
 * ServerInfo info = ServerInfo.build();
 * </pre>
 *
 * The server name and version will be fetched from the runtime properties:
 * {@link Environment#DISTRIBUTION_NAME} and
 * {@link Environment#DISTRIBUTION_VERSION} If you want to use another name and
 * version just call {@link #build(String, String)} instead to build your server
 * information.
 * <p>
 * After building a <code>ServerInfo</code> object you can start browsing the
 * bundles deployed on the server by calling {@link #getBundles()} or fetch a
 * specific bundle given its symbolic name {@link #getBundle(String)}.
 * <p>
 * To write down the server information as XML use {@link #toXML(Writer)} and to
 * read it back use {@link #fromXML(Reader)}.
 * <p>
 * Example:
 *
 * <pre>
 * ServerInfo info = ServerInfo.build();
 * BundleInfo binfo =info.getBundle("org.nuxeo.runtime");
 * System.out.println("Bundle Id: "+binfo.getBundleId());
 * System.out.println("File Name: "+binfo.getFileName());
 * System.out.println("Manifest: "+ binfo.getManifest());
 * for (ComponentInfo cinfo : binfo.getComponents()) {
 *   System.out.println("Component: "+cinfo.getName());
 *   System.out.println(cinfo.getDocumentation());
 *   // find extension points provided by this component
 *   for (ExtensionPointInfo xpi : cinfo.getExtensionPoints()) {
 *     System.out.println("Extension point: "+xpi.getName());
 *     System.out.println("Accepted contribution classes: "+Arrays.asList(xpi.getTypes()));
 *     // find contributed extensions to this extension point:
 *
 *   }
 *   // find contribution provided by this component
 *   for (ExtensionInfo xi : cinfo.getExtensions()) {
 *      System.out.println("Extension: "+xi.getId()+" to "+xi.getExtensionPoint());
 *      System.out.println(xi.getDocumentation());
 *      ...
 *   }
 * }
 * </pre>
 */
public class ServerInfo {

    private static final Log log = LogFactory.getLog(ServerInfo.class);

    public static final String META_INF_MANIFEST_MF = "META-INF/MANIFEST.MF";

    public static final String POM_XML = "pom.xml";

    public static final String POM_PROPERTIES = "pom.properties";

    protected static final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

    protected static final XPathFactory xpathFactory = XPathFactory.newInstance();

    protected final String name;

    protected final String version;

    protected final Map<String, BundleInfo> bundles = new HashMap<>();

    protected final List<Class<?>> allSpi = new ArrayList<>();

    public ServerInfo(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public ServerInfo(@JsonProperty("name") String name, @JsonProperty("version") String version, @JsonProperty("bundles") Set<BundleInfo> bundles) {
        this(name, version);
        for (BundleInfo bundle : bundles) {
            this.bundles.put(bundle.getBundleId(), bundle);
        }
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public Collection<BundleInfo> getBundles() {
        return bundles.values();
    }

    public void addBundle(BundleInfo bundle) {
        bundles.put(bundle.getId(), bundle);
    }

    public void addBundle(Collection<BundleInfo> someBundles) {
        for (BundleInfo bundle : someBundles) {
            bundles.put(bundle.getId(), bundle);
        }
    }

    public BundleInfo getBundle(String id) {
        return bundles.get(id);
    }

    public static ServerInfo build() {
        return build(Framework.getProperty(Environment.DISTRIBUTION_NAME, "Nuxeo"),
                Framework.getProperty(Environment.DISTRIBUTION_VERSION, "unknown"));
    }

    protected static BundleInfoImpl computeBundleInfo(Bundle bundle) {
        RuntimeService runtime = Framework.getRuntime();
        BundleInfoImpl binfo = new BundleInfoImpl(bundle.getSymbolicName());
        binfo.setFileName(runtime.getBundleFile(bundle).getName());
        binfo.setLocation(bundle.getLocation());
        if (!(bundle instanceof BundleImpl)) {
            return binfo;
        }
        BundleImpl nxBundle = (BundleImpl) bundle;
        File jarFile = nxBundle.getBundleFile().getFile();
        if (jarFile == null) {
            return binfo;
        }
        try {
            if (jarFile.isDirectory()) {
                // directory: run from Eclipse in unit tests
                // .../nuxeo-runtime/nuxeo-runtime/bin
                // or sometimes
                // .../nuxeo-runtime/nuxeo-runtime/bin/main
                File manifest = new File(jarFile, META_INF_MANIFEST_MF);
                if (manifest.exists()) {
                    InputStream is = new FileInputStream(manifest);
                    String mf = IOUtils.toString(is, StandardCharsets.UTF_8);
                    binfo.setManifest(mf);
                }
                // find and parse pom.xml
                File up = new File(jarFile, "..");
                File pom = new File(up, POM_XML);
                if (!pom.exists()) {
                    pom = new File(new File(up, ".."), POM_XML);
                    if (!pom.exists()) {
                        pom = null;
                    }
                }
                if (pom != null) {
                    DocumentBuilder b = documentBuilderFactory.newDocumentBuilder();
                    Document doc = b.parse(new FileInputStream(pom));
                    XPath xpath = xpathFactory.newXPath();
                    String groupId = (String) xpath.evaluate("//project/groupId", doc, XPathConstants.STRING);
                    if ("".equals(groupId)) {
                        groupId = (String) xpath.evaluate("//project/parent/groupId", doc, XPathConstants.STRING);
                    }
                    String artifactId = (String) xpath.evaluate("//project/artifactId", doc, XPathConstants.STRING);
                    if ("".equals(artifactId)) {
                        artifactId = (String) xpath.evaluate("//project/parent/artifactId", doc, XPathConstants.STRING);
                    }
                    String version = (String) xpath.evaluate("//project/version", doc, XPathConstants.STRING);
                    if ("".equals(version)) {
                        version = (String) xpath.evaluate("//project/parent/version", doc, XPathConstants.STRING);
                    }
                    binfo.setArtifactId(artifactId);
                    binfo.setGroupId(groupId);
                    binfo.setArtifactVersion(version);
                }
            } else {
                try (ZipFile zFile = new ZipFile(jarFile)) {
                    ZipEntry mfEntry = zFile.getEntry(META_INF_MANIFEST_MF);
                    if (mfEntry != null) {
                        try (InputStream mfStream = zFile.getInputStream(mfEntry)) {
                            String mf = IOUtils.toString(mfStream, StandardCharsets.UTF_8);
                            binfo.setManifest(mf);
                        }
                    }
                    Enumeration<? extends ZipEntry> entries = zFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        if (entry.getName().endsWith(POM_PROPERTIES)) {
                            try (InputStream is = zFile.getInputStream(entry)) {
                                PropertyResourceBundle prb = new PropertyResourceBundle(is);
                                String groupId = prb.getString("groupId");
                                String artifactId = prb.getString("artifactId");
                                String version = prb.getString("version");
                                binfo.setArtifactId(artifactId);
                                binfo.setGroupId(groupId);
                                binfo.setArtifactVersion(version);
                            }
                            break;
                        }
                    }
                }
                try (ZipFile zFile = new ZipFile(jarFile)) {
                    EmbeddedDocExtractor.extractEmbeddedDoc(zFile, binfo);
                }
            }
        } catch (IOException | ParserConfigurationException | SAXException | XPathException | NuxeoException e) {
            log.error(e, e);
        }
        return binfo;
    }

    protected static List<Class<?>> getSPI(Class<?> klass) {
        List<Class<?>> spi = new ArrayList<>();
        for (Field field : klass.getDeclaredFields()) {
            String cName = field.getType().getCanonicalName();
            if (cName.startsWith("org.nuxeo")) {
                // remove XObjects
                Class<?> fieldClass = field.getType();
                Annotation[] annotations = fieldClass.getDeclaredAnnotations();
                if (annotations.length == 0) {
                    spi.add(fieldClass);
                }
            }
        }
        return spi;
    }

    public static ServerInfo build(String name, String version) {
        RuntimeService runtime = Framework.getRuntime();
        ServerInfo server = new ServerInfo(name, version);
        BundleInfoImpl configVirtualBundle = new BundleInfoImpl("org.nuxeo.ecm.config");
        server.addBundle(configVirtualBundle);

        Map<String, ExtensionPointInfoImpl> xpRegistry = new HashMap<>();
        List<ExtensionInfoImpl> contribRegistry = new ArrayList<>();

        Collection<RegistrationInfo> registrations = runtime.getComponentManager().getRegistrations();

        for (RegistrationInfo ri : registrations) {
            String cname = ri.getName().getName();
            Bundle bundle = ri.getContext().getBundle();
            BundleInfoImpl binfo = null;

            if (bundle == null) {
                binfo = configVirtualBundle;
            } else {
                String symName = bundle.getSymbolicName();
                if (symName == null) {
                    log.error("No symbolic name found for bundle " + cname);
                    continue;
                }
                // avoids duplicating/overriding the bundles
                if (server.bundles.containsKey(bundle.getSymbolicName())) {
                    binfo = (BundleInfoImpl) server.bundles.get(bundle.getSymbolicName());
                } else {
                    binfo = computeBundleInfo(bundle);
                }
            }

            // TODO binfo.setRequirements(requirements);
            ComponentInfoImpl component = new ComponentInfoImpl(binfo, cname);
            if (ri.getExtensionPoints() != null) {
                for (ExtensionPoint xp : ri.getExtensionPoints()) {
                    ExtensionPointInfoImpl xpinfo = new ExtensionPointInfoImpl(component, xp.getName());
                    Class<?>[] ctypes = xp.getContributions();
                    String[] descriptors = new String[ctypes.length];

                    for (int i = 0; i < ctypes.length; i++) {
                        descriptors[i] = ctypes[i].getCanonicalName();
                        List<Class<?>> spi = getSPI(ctypes[i]);
                        xpinfo.addSpi(spi);
                        server.allSpi.addAll(spi);
                    }
                    xpinfo.setDescriptors(descriptors);
                    xpinfo.setDocumentation(xp.getDocumentation());
                    xpRegistry.put(xpinfo.getId(), xpinfo);
                    component.addExtensionPoint(xpinfo);
                }
            }

            component.setXmlFileUrl(ri.getXmlFileUrl());

            if (ri.getProvidedServiceNames() != null) {
                for (String serviceName : ri.getProvidedServiceNames()) {
                    component.addService(serviceName, isServiceOverriden(ri, serviceName));
                }
            }

            if (ri.getExtensions() != null) {
                for (Extension xt : ri.getExtensions()) {
                    ExtensionInfoImpl xtinfo = new ExtensionInfoImpl(component, xt.getExtensionPoint());
                    xtinfo.setTargetComponentName(xt.getTargetComponent());
                    xtinfo.setContribution(xt.getContributions());
                    xtinfo.setDocumentation(xt.getDocumentation());
                    xtinfo.setXml(DocumentationHelper.secureXML(xt.toXML()));

                    contribRegistry.add(xtinfo);

                    component.addExtension(xtinfo);
                }
            }

            component.setComponentClass(ri.getImplementation());
            component.setDocumentation(ri.getDocumentation());

            binfo.addComponent(component);
            server.addBundle(binfo);
        }

        // now register the bundles that contains no components !!!
        Bundle[] allbundles = runtime.getContext().getBundle().getBundleContext().getBundles();
        for (Bundle bundle : allbundles) {
            if (!server.bundles.containsKey(bundle.getSymbolicName())) {
                BundleInfo bi = computeBundleInfo(bundle);
                server.addBundle(bi);
            }
        }

        // associate contrib to XP
        for (ExtensionInfoImpl contrib : contribRegistry) {
            String xp = contrib.getExtensionPoint();
            ExtensionPointInfoImpl ep = xpRegistry.get(xp);
            if (ep != null) {
                ep.addExtension(contrib);
            }
        }

        return server;
    }

    protected static boolean isServiceOverriden(RegistrationInfo ri, String serviceName) {
        try {
            Class<?> typeof = Class.forName(serviceName);
            final Object adapter = ri.getComponent().getAdapter(typeof);
            final Object service = Framework.getService(typeof);
            if (adapter == service) {
                return false;
            }
            return service.getClass() != adapter.getClass();
        } catch (ClassNotFoundException cause) {
            return false;
        } catch (NullPointerException cause) {
            return false;
        }
    }

    public void toXML(Writer writer) throws IOException {
        XMLWriter xw = new XMLWriter(writer, 4);
        xw.start();
        xw.element("server").attr("name", name).attr("version", version).start();
        for (BundleInfo bundle : bundles.values()) {
            xw.element("bundle").attr("id", bundle.getId()).start();
            xw.element("fileName").content(bundle.getFileName());
            // TODO requirements
            for (ComponentInfo component : bundle.getComponents()) {
                xw.element("component").attr("id", component.getId()).start();
                // for (ExtensionPointInfo xp : component.getExtensionPoints())
                // { }
                // for (ExtensionInfo xt : component.getExtensions()) { }
                xw.close();
            }
            xw.close();
        }
        xw.close();
        xw.close();
    }

    public static ServerInfo fromXML(File file) throws IOException {
        InputStreamReader reader = new FileReader(file);
        try {
            return fromXML(reader);
        } finally {
            reader.close();
        }
    }

    public static ServerInfo fromXML(Reader reader) {
        return null;
    }

    public List<Class<?>> getAllSpi() {
        return allSpi;
    }

}
