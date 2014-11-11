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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.jbpm.core.deployer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public class MD5Hasher implements Serializable {

    private static final long serialVersionUID = 1L;

    public DocumentBuilder getDocumentBuider()
            throws ParserConfigurationException {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    public Transformer getTransformer()
            throws TransformerConfigurationException,
            TransformerFactoryConfigurationError {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        return transformer;
    }

    public String getMD5FromURL(URL url) throws SAXException, IOException,
            TransformerException, NoSuchAlgorithmException {
        Document document = getDomDocument(url);
        byte[] bytes = getBytes(trimDocument(document));
        return MD5(bytes);
    }

    public String MD5(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(bytes);
        byte[] md5hash = md.digest();
        StringBuilder builder = new StringBuilder();
        for (byte b : md5hash) {
            builder.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return builder.toString();
    }

    public byte[] getBytes(Document document) throws IOException,
            TransformerException {
        File file = File.createTempFile("nuxeo", "ifChangedDeployer.xml");
        Source source = new DOMSource(document);
        Result result = new StreamResult(file);
        getTransformer().transform(source, result);
        FileInputStream stream = new FileInputStream(file);
        byte[] bytes = new byte[(int) file.length()];
        stream.read(bytes);
        return bytes;
    }

    public Document getDomDocument(URL url) throws SAXException, IOException {
        assert url != null;
        File file = new File(url.getPath());
        try {
            return getDocumentBuider().parse(file);
        } catch (ParserConfigurationException e) {
            throw new IOException(e.getMessage());
        }
    }

    public Document trimDocument(Document document) {
        trimNode(document.getFirstChild());
        return document;
    }

    private void trimNode(Node node) {
        Node child = node.getFirstChild();
        while (child != null) {
            Node next = child.getNextSibling();
            if (child instanceof Comment) {
                node.removeChild(child);
                node.normalize();
            } else if (child instanceof Text
                    && ((Text) child).getNodeValue().matches("\\s+")) {
                node.removeChild(child);
                node.normalize();
            } else {
                trimNode(child);
            }
            child = next;
        }
    }
}
