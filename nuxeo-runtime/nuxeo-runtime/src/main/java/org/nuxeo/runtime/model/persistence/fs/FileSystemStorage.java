/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    public static final Log log = LogFactory.getLog(FileSystemStorage.class);

    protected static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    protected final File root;

    public FileSystemStorage() {
        root = new File(Environment.getDefault().getData(), "contribs");
        root.mkdirs();
    }

    public static synchronized String safeRead(File file) throws IOException {
        return FileUtils.readFile(file);
    }

    public static synchronized void safeWrite(File file, String content)
            throws IOException {
        FileUtils.writeFile(file, content);
    }

    public static synchronized boolean safeCreate(File file, String content)
            throws IOException {
        if (file.isFile()) {
            return false;
        }
        FileUtils.writeFile(file, content);
        return true;
    }

    public static synchronized boolean safeRemove(File file) throws IOException {
        return file.delete();
    }

    public static void loadMetadata(Contribution contrib) {
        try {
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(new ByteArrayInputStream(
                    contrib.getContent().getBytes()));
            Element root = doc.getDocumentElement();
            contrib.setDisabled(Boolean.parseBoolean(root.getAttribute("disabled")));
            Node node = root.getFirstChild();
            while (node != null) {
                if (node.getNodeType() == Node.ELEMENT_NODE
                        && "documentation".equals(node.getNodeName())) {
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
        } catch (Exception e) {
            log.error("Failed to read contribution metadata", e);
        }
    }

    @Override
    public Contribution addContribution(Contribution contribution)
            throws Exception {
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
    public boolean removeContribution(Contribution contrib) throws Exception {
        return safeRemove(new File(root, contrib.getName() + ".xml"));
    }

    @Override
    public Contribution updateContribution(Contribution contribution)
            throws Exception {
        File file = new File(root, contribution.getName() + ".xml");
        String content = safeRead(file);
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
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
        el.appendChild(doc.createTextNode(description));
        root.appendChild(el);

        safeWrite(file, DOMSerializer.toString(doc));
        return getContribution(contribution.getName());
    }

}
