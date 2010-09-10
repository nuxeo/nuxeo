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
package org.nuxeo.runtime.model.persistence.fs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.xmap.DOMSerializer;
import org.nuxeo.runtime.model.persistence.Contribution;
import org.nuxeo.runtime.model.persistence.ContributionStorage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class FileSystemStorage implements ContributionStorage {

    protected static Pattern DOC_PATTERN = Pattern.compile("<documentation>(.*)</documentation>");

    protected static Pattern DISABLED_PATTERN = Pattern.compile("disabled\\s*=\\s*([^\\s>]+)");

    protected File root;

    public FileSystemStorage() {
        root = new File(Environment.getDefault().getData(), "contribs");
        root.mkdirs();
    }

    public synchronized static String safeRead(File file) throws IOException {
        return FileUtils.readFile(file);
    }

    public synchronized static void safeWrite(File file, String content)
            throws IOException {
        FileUtils.writeFile(file, content);
    }

    public synchronized static boolean safeCreate(File file, String content)
            throws IOException {
        if (file.isFile()) {
            return false;
        }
        FileUtils.writeFile(file, content);
        return true;
    }

    public synchronized static boolean safeRemove(File file) throws IOException {
        return file.delete();
    }

    public static String readDocumentation(String content) {
        Matcher m = DOC_PATTERN.matcher(content);
        if (m.find()) {
            return m.group(1).trim();
        }
        return "";
    }

    public static boolean readDisabledFlag(String content) {
        int i = content.indexOf("component");
        int j = content.indexOf(">", i);
        if (i == -1 || j == -1) {
            throw new IllegalArgumentException("Invalid component content.");
        }
        content = content.substring(i, j);

        Matcher m = DISABLED_PATTERN.matcher(content);
        if (m.find()) {
            String v = m.group(1).trim().toLowerCase();
            return v.contains("true");
        }
        return false;
    }

    public Contribution addContribution(Contribution contribution)
            throws Exception {
        File file = new File(root, contribution.getName() + ".xml");
        String content = contribution.getContent();
        if (safeCreate(file, content)) {
            return new ContributionFile(contribution.getName(), file);
        }
        return null;
    }

    public Contribution getContribution(String name) {
        File file = new File(root, name + ".xml");
        if (file.isFile()) {
            return new ContributionFile(name, file);
        }
        return null;
    }

    public List<Contribution> getContributions() {
        ArrayList<Contribution> result = new ArrayList<Contribution>();
        File[] files = root.listFiles();
        if (files == null) {
            return result;
        }
        for (File file : files) {
            String name = file.getName();
            if (name.endsWith(".xml")) {
                name = name.substring(0, name.length() - 4);
                result.add(new ContributionFile(name, file));
            }
        }
        return result;
    }

    public boolean removeContribution(Contribution contrib) throws Exception {
        return safeRemove(new File(root, contrib.getName() + ".xml"));
    }

    public Contribution updateContribution(Contribution contribution)
            throws Exception {
        File file = new File(root, contribution.getName() + ".xml");
        String content = safeRead(file);
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = docBuilder.parse(new ByteArrayInputStream(
                content.getBytes()));
        Element root = doc.getDocumentElement();
        if (contribution.isDisabled()) {
            root.setAttribute("disabled", "true");
        } else {
            root.removeAttribute("disabled");
        }
        Node node = root.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE
                    && "documentation".equals(node.getNodeName())) {
                break;
            }
            node = node.getNextSibling();
        }
        String description = contribution.getDescription();
        if (description == null) {
            description = "";
        }
        if (node != null) {
            root.removeChild(node);
        }
        Element el = doc.createElement("documentation");
        el.appendChild(doc.createTextNode(contribution.getDescription()));
        root.appendChild(el);

        safeWrite(file, DOMSerializer.toString(doc));
        return getContribution(contribution.getName());
    }

}
