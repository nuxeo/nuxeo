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
 *     bstefanescu
 */
package org.nuxeo.runtime.model.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.serialize.Method;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.nuxeo.runtime.Version;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.ExtensionPoint;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
// use by reflection from DevFrameworkBootstrap
public class ComponentRegistrySerializer {

    private final static Log log = LogFactory.getLog(ComponentRegistrySerializer.class);

    public static void writeIndex(File file) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        try {
            writeIndex(writer);
        } finally {
            writer.close();
        }
    }

    public static void writeIndex(Writer writer) throws IOException {
        ComponentManagerImpl mgr = (ComponentManagerImpl) Framework.getRuntime().getComponentManager();
        for (RegistrationInfo ri : mgr.getRegistrations()) {
            ComponentName name = ri.getName();
            if (name == null) {
                log.error("BUG: Found component with null name");
                continue;
            }
            String src = getComponentSrc(ri);
            if (src == null) {
                src = "";
            }

            String bundle = ri.getBundle();
            if (bundle == null) {
                bundle = ri.getContext().getBundle().getSymbolicName();
            }
            if (bundle == null) {
                bundle = "";
            }

            String cname = name.getName();
            writer.write("c:");
            writer.write(cname);
            writer.write(":");
            writer.write(bundle);
            writer.write(":");
            writer.write(src);
            writer.write("\n");

            // write services
            String[] services = ri.getProvidedServiceNames();
            if (services != null && services.length > 0) {
                for (String service : services) {
                    writer.write("s:");
                    writer.write(cname);
                    writer.write(":");
                    writer.write(service);
                    writer.write("\n");
                }
            }

            // write services
            ExtensionPoint[] xpoints = ri.getExtensionPoints();
            if (xpoints != null && xpoints.length > 0) {
                for (ExtensionPoint xpoint : xpoints) {
                    writer.write("x:");
                    writer.write(cname);
                    writer.write(":");
                    writer.write(xpoint.getName());
                    writer.write("\n");
                }
            }
        }
    }

    private static String getComponentSrc(RegistrationInfo ri) {
        URL url = ri.getXmlFileUrl();
        if (url != null) {
            String src;
            String path = url.toExternalForm();
            int i = path.lastIndexOf('!');
            if (i > 0) {
                String jar = path.substring(0, i);
                path = path.substring(i + 1);
                int s = jar.lastIndexOf('/');
                if (s > -1) {
                    jar = jar.substring(s + 1);
                }
                src = jar + "!" + path;
            } else {
                int s = path.lastIndexOf('/');
                if (s != -1) {
                    src = path.substring(s + 1);
                } else {
                    src = path;
                }
            }
            return src;
        }
        return null;
    }

    public static Document toDocument() {
        ComponentManagerImpl mgr = (ComponentManagerImpl) Framework.getRuntime().getComponentManager();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc;
        try {
            doc = factory.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        Element root = doc.createElement("components");
        doc.appendChild(root);

        for (RegistrationInfo ri : mgr.getRegistrations()) {
            ComponentName name = ri.getName();
            if (name == null) {
                log.error("BUG: Found component with null name");
                continue;
            }

            Element comp = doc.createElement("component");
            comp.setAttribute("name", name.getName());
            String impl = ri.getImplementation();
            if (impl != null && impl.length() > 0) {
                comp.setAttribute("class", impl);
            }
            String bundle = ri.getBundle();
            if (bundle == null) {
                bundle = ri.getContext().getBundle().getSymbolicName();
            }
            if (bundle != null) {
                comp.setAttribute("bundle", bundle);
            }
            Version v = ri.getVersion();
            if (v != null) {
                comp.setAttribute("version", v.toString());
            }
            root.appendChild(comp);

            // write source if known
            String src = getComponentSrc(ri);
            if (src != null) {
                comp.setAttribute("src", src);
            }

            // write documentation
            String docText = ri.getDocumentation();
            if (docText != null) {
                docText = docText.trim();
                Element docu = doc.createElement("documentation");
                docu.setTextContent(docText);
                comp.appendChild(docu);
            }

            // write services
            String[] services = ri.getProvidedServiceNames();
            if (services != null && services.length > 0) {
                Element svcsEl = doc.createElement("services");
                for (String service : services) {
                    Element svcEl = doc.createElement("service");
                    svcEl.setAttribute("class", service);
                    svcsEl.appendChild(svcEl);
                }
                comp.appendChild(svcsEl);
            }

            // write extension points
            ExtensionPoint[] xps = ri.getExtensionPoints();
            if (xps != null && xps.length > 0) {
                Element xpsEl = doc.createElement("extension-points");
                for (ExtensionPoint xp : xps) {
                    Element xpEl = doc.createElement("extension-point");
                    xpEl.setAttribute("name", xp.getName());
                    docText = xp.getDocumentation();
                    if (docText != null) {
                        xpEl.setTextContent(docText.trim());
                    }
                    xpsEl.appendChild(xpEl);
                }
                comp.appendChild(xpsEl);
            }
        }
        return doc;
    }

    public static void toXML(OutputStream out) throws IOException {
        toXML(out, "UTF-8");
    }

    public static void toXML(OutputStream out, String encoding) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(out, encoding);
        toXML(writer, encoding);
    }

    public static void toXML(Writer out) throws IOException {
        toXML(out, "UTF-8");
    }

    public static void toXML(Writer out, String encoding) throws IOException {
        Document doc = toDocument();
        OutputFormat format = new OutputFormat(Method.XML, encoding, true);
        format.setIndent(2);
        XMLSerializer serializer = new XMLSerializer(out, format);
        serializer.serialize(doc);
        out.flush();
    }

    public static String toXML() throws IOException {
        StringWriter writer = new StringWriter();
        toXML(writer);
        return writer.toString();
    }

}
