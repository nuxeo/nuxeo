/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.runtime.model.impl;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;

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
 * 
 */
public class ComponentRegistrySerializer {

    private final static Log log = LogFactory.getLog(ComponentRegistrySerializer.class);

    public static Document toDocument() throws Exception {
        ComponentManagerImpl mgr = (ComponentManagerImpl) Framework.getRuntime().getComponentManager();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = factory.newDocumentBuilder().newDocument();
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

    public static void toXML(OutputStream out) throws Exception {
        toXML(out, "UTF-8");
    }

    public static void toXML(OutputStream out, String encoding)
            throws Exception {
        OutputStreamWriter writer = new OutputStreamWriter(out, encoding);
        toXML(writer, encoding);
    }

    public static void toXML(Writer out) throws Exception {
        toXML(out, "UTF-8");
    }

    public static void toXML(Writer out, String encoding) throws Exception {
        Document doc = toDocument();
        OutputFormat format = new OutputFormat(Method.XML, encoding, true);
        format.setIndent(2);
        XMLSerializer serializer = new XMLSerializer(out, format);
        serializer.serialize(doc);
        out.flush();
    }

    public static String toXML() throws Exception {
        StringWriter writer = new StringWriter();
        toXML(writer);
        return writer.toString();
    }

}
