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
package org.nuxeo.build.osgi.gen;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PomLoader {


    protected File file;
    protected Document doc;

    public PomLoader(File file) throws Exception {
        this.file = file;
        FileInputStream in = new FileInputStream(file);
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.parse(in);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
        }

    }

    public List<File> getModuleFiles() {
        Element modules = getModules();
        return modules != null ? readModules(modules) : null;
    }

    protected List<File> readModules(Element root) {
        File dir = file.getParentFile();
        ArrayList<File> modules = new ArrayList<File>();
        Node node = root.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if ("module".equals(node.getNodeName())) {
                    String path = node.getTextContent().trim();
                    modules.add(new File(dir, path));
                }
            }
            node = node.getNextSibling();
        }
        return modules;
    }


    protected Element getFirstElement(Element root, String name) {
        Node node = root.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (name.equals(node.getNodeName())) {
                    return (Element)node;
                }
            }
            node = node.getNextSibling();
        }
        return null;
    }

    public String getGroupId() {
        Element el = getFirstElement(doc.getDocumentElement(), "groupId");
        return el != null ? el.getTextContent().trim() : getParentGroupId();
    }

    public Element getParentElement() {
        return getFirstElement(doc.getDocumentElement(), "parent");
    }

    public String getParentGroupId() {
        Element el = getParentElement();
        if (el != null) {
            el = getFirstElement(el, "groupId");
            return el != null ? el.getTextContent().trim() : null;
        }
        return null;
    }

    public String getArtifactId() {
        Element el = getFirstElement(doc.getDocumentElement(), "artifactId");
        return el != null ? el.getTextContent().trim() : null;
    }

    public String getVersion() {
        Element el = getFirstElement(doc.getDocumentElement(), "version");
        if (el == null) {
            el = getParentElement();
            if (el != null) {
                el = getFirstElement(el, "version");
            }
        }
        return el != null ? el.getTextContent().trim() : null;
    }

    public String getPackaging() {
        Element el = getFirstElement(doc.getDocumentElement(), "packaging");
        return el != null ? el.getTextContent().trim() : null;
    }

    public String getProjectName() {
        Element el = getFirstElement(doc.getDocumentElement(), "name");
        return el != null ? el.getTextContent().trim() : null;
    }

    public String getProjectDescription() {
        Element el = getFirstElement(doc.getDocumentElement(), "description");
        return el != null ? el.getTextContent().trim() : "";
    }

    public Element getModules() {
        return getFirstElement(doc.getDocumentElement(), "modules");
    }


}
