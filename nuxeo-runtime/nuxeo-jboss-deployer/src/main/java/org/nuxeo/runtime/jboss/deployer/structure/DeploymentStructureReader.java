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
package org.nuxeo.runtime.jboss.deployer.structure;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.virtual.VirtualFile;
import org.nuxeo.runtime.jboss.deployer.Utils;
import org.nuxeo.runtime.jboss.deployer.structure.DeploymentStructure.Context;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DeploymentStructureReader {

    protected static Log log = LogFactory.getLog(DeploymentStructureReader.class);

    protected DocumentBuilderFactory dbfac;

    public DeploymentStructureReader() {
        dbfac = DocumentBuilderFactory.newInstance();
    }

    public DeploymentStructure read(VirtualFile vhome, InputStream in)
            throws Exception {
        DeploymentStructure md = new DeploymentStructure(vhome);
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.parse(in);
        Element root = doc.getDocumentElement();
        Attr attr = root.getAttributeNode("children");
        if (attr != null) {
            String[] ar = Utils.split(attr.getValue().trim(), ':', true);
            md.setChildren(ar);
        }
        attr = root.getAttributeNode("bundles");
        if (attr != null) {
            String[] ar = Utils.split(attr.getValue().trim(), ':', true);
            md.setBundles(ar);
        }
        Node node = root.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String name = node.getNodeName().toLowerCase();
                if ("context".equalsIgnoreCase(name)) {
                    readContext((Element) node, md);
                } else if ("properties".equals(name)) {
                    readProperties(vhome, (Element) node, md);
                } else if ("preprocessor".equals(name)) {
                    readPreprocessor((Element) node, md);
                }
            }
            node = node.getNextSibling();
        }
        return md;
    }

    protected void readPreprocessor(Element element, DeploymentStructure md) {
        Attr attr = element.getAttributeNode("enabled");
        String enabled = attr == null ? "true" : attr.getValue().trim();
        md.setRequirePreprocessing(Boolean.parseBoolean(enabled));
        attr = element.getAttributeNode("classpath");
        if (attr != null) {
            String[] ar = Utils.split(attr.getValue().trim(), ':', true);
            md.setPreprocessorClassPath(ar);
        }
    }

    protected void readContext(Element element, DeploymentStructure md) {
        Attr attr = element.getAttributeNode("path");
        String path = attr == null ? "" : attr.getValue().trim();
        DeploymentStructure.Context ctx = new Context(path);
        attr = element.getAttributeNode("metaDataPath");
        if (attr != null) {
            String[] ar = Utils.split(attr.getValue().trim(), ':', true);
            ctx.setMetaDataPath(ar);
        }
        attr = element.getAttributeNode("classpath");
        if (attr != null) {
            String[] ar = Utils.split(attr.getValue().trim(), ':', true);
            ctx.setClasspath(ar);
        }
        md.addContext(ctx);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void readProperties(VirtualFile file, Element element,
            DeploymentStructure md) throws Exception {
        Attr attr = element.getAttributeNode("src");
        if (attr != null) {
            VirtualFile vf = file.getChild(attr.getValue().trim());
            if (vf != null) {
                InputStream in = vf.openStream();
                try {
                    Properties props = new Properties();
                    props.load(in);
                    md.getProperties().putAll((Map) props);
                } finally {
                    in.close();
                }
            } else {
                log.warn("Properties file referenced in nuxeo-structure.xml could not be found: "
                        + attr.getValue());
            }
        }
        // load contents too if any
        Node child = element.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                if (child.getNodeName().equalsIgnoreCase("property")) {
                    Element echild = (Element) child;
                    attr = echild.getAttributeNode("name");
                    if (attr == null) {
                        log.warn("Invalid property element format in nuxeo-structure.xml. Property name attribute is required");
                    }
                    md.setProperty(attr.getValue().trim(),
                            echild.getTextContent().trim());
                }
            }
            child = child.getNextSibling();
        }
    }
}
