/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.model.persistence.fs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.common.xmap.DOMSerializer;
import org.nuxeo.runtime.model.persistence.Contribution;
import org.nuxeo.runtime.model.persistence.ContributionStorage;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FileSystemStorage implements ContributionStorage {

    public static final Log log = LogFactory.getLog(FileSystemStorage.class);

    protected static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    protected final File root;

    public FileSystemStorage() {
        root = new File(Environment.getDefault().getData(), "contribs");
        root.mkdirs();
    }

    public static synchronized String safeRead(File file) {
        try {
            return FileUtils.readFileToString(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static synchronized void safeWrite(File file, String content) {
        try {
            FileUtils.writeStringToFile(file, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static synchronized boolean safeCreate(File file, String content) {
        if (file.isFile()) {
            return false;
        }
        try {
            FileUtils.writeStringToFile(file, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public static synchronized boolean safeRemove(File file) {
        return file.delete();
    }

    public static void loadMetadata(Contribution contrib) {
        try {
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(new ByteArrayInputStream(contrib.getContent().getBytes()));
            Element root = doc.getDocumentElement();
            contrib.setDisabled(Boolean.parseBoolean(root.getAttribute("disabled")));
            Node node = root.getFirstChild();
            while (node != null) {
                if (node.getNodeType() == Node.ELEMENT_NODE && "documentation".equals(node.getNodeName())) {
                    break;
                }
                node = node.getNextSibling();
            }
            if (node != null) {
                node = node.getFirstChild();
                StringBuilder buf = new StringBuilder();
                while (node != null) {
                    if (node.getNodeType() == Node.TEXT_NODE) {
                        buf.append(node.getNodeValue());
                    }
                    node = node.getNextSibling();
                }
                contrib.setDescription(buf.toString().trim());
            } else {
                contrib.setDescription("");
            }
        } catch (ParserConfigurationException | SAXException | IOException | DOMException e) {
            log.error("Failed to read contribution metadata", e);
        }
    }

    @Override
    public Contribution addContribution(Contribution contribution) {
        File file = new File(root, contribution.getName() + ".xml");
        String content = contribution.getContent();
        if (safeCreate(file, content)) {
            return new ContributionFile(contribution.getName(), file);
        }
        return null;
    }

    @Override
    public Contribution getContribution(String name) {
        File file = new File(root, name + ".xml");
        if (file.isFile()) {
            return new ContributionFile(name, file);
        }
        return null;
    }

    @Override
    public List<Contribution> getContributions() {
        List<Contribution> result = new ArrayList<Contribution>();
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

    @Override
    public boolean removeContribution(Contribution contrib) {
        return safeRemove(new File(root, contrib.getName() + ".xml"));
    }

    @Override
    public Contribution updateContribution(Contribution contribution) {
        File file = new File(root, contribution.getName() + ".xml");
        String content = safeRead(file);
        DocumentBuilder docBuilder;
        try {
            docBuilder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        Document doc;
        try {
            doc = docBuilder.parse(new ByteArrayInputStream(content.getBytes()));
        } catch (SAXException | IOException e) {
            throw new RuntimeException(e);
        }
        Element root = doc.getDocumentElement();
        if (contribution.isDisabled()) {
            root.setAttribute("disabled", "true");
        } else {
            root.removeAttribute("disabled");
        }
        Node node = root.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE && "documentation".equals(node.getNodeName())) {
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
        el.appendChild(doc.createTextNode(description));
        root.appendChild(el);

        try {
            safeWrite(file, DOMSerializer.toString(doc));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return getContribution(contribution.getName());
    }

}
