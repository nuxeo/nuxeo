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
package org.nuxeo.runtime.jboss.deployer;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class Pack {

    public static void main(String[] args) throws Exception {

        File wd = new File(".");
        File file = new File(wd, "META-INF/application.xml");
        if (!file.isFile()) {
            System.err.println("You should run this tool from a preprocessed nuxeo.ear folder");
        }
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        FileInputStream in = new FileInputStream(file);
        Document doc = docBuilder.parse(in);
        Element root = doc.getDocumentElement();
        NodeList list = root.getElementsByTagName("module");
        ArrayList<String> paths = new ArrayList<String>();
        for (int i = 0; i < list.getLength(); i++) {
            Element el = (Element) list.item(i);
            Node n = el.getFirstChild();
            while (n != null) {
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    Element mtype = ((Element) n);
                    String type = n.getNodeName().toLowerCase();
                    String path = null;
                    if ("web".equals(type)) {
                        path = ((Element) (mtype.getElementsByTagName("web-uri").item(0))).getTextContent().trim();
                    } else {
                        path = mtype.getTextContent().trim();
                    }
                    paths.add(path);
                }
                n = n.getNextSibling();
            }
        }

        File ejbs = new File(wd, "tmp-ejbs");
        ejbs.mkdirs();
        for (String path : paths) {
            System.out.println("Move EAR module " + path + " to "
                    + ejbs.getName());
            File f = new File(wd, path);
            f.renameTo(new File(ejbs, f.getName()));
        }
        File lib = new File(wd, "lib");
        File[] files = new File(wd, "bundles").listFiles();
        if (files != null) {
            for (File f : files) {
                System.out.println("Move POJO bundle " + f.getName()
                        + " to lib");
                f.renameTo(new File(lib, f.getName()));
            }
        }
        File bundles = new File(wd, "bundles");
        files = ejbs.listFiles();
        if (files != null) {
            for (File f : files) {
                System.out.println("Move back EAR module " + f.getName()
                        + " to bundles");
                f.renameTo(new File(bundles, f.getName()));
            }
        }
    }

}
