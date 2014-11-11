/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Descriptor for the configuration of a binaries storage.
 *
 * @author Florent Guillaume
 */
@XObject(value = BinaryManagerDescriptor.BINARY_STORE)
public class BinaryManagerDescriptor {

    public static final String BINARY_STORE = "binary-store";

    public static final String DIGEST = "digest";

    public static final String DEPTH = "depth";

    /**
     * The digest, for instance {@code MD5} or {@code SHA-256}.
     */
    @XNode(DIGEST)
    public String digest;

    /**
     * The number of intermediate sub-directories to use.
     */
    @XNode(DEPTH)
    public int depth;

    /**
     * Writes the descriptor to an XML file.
     *
     * @param out the output file to use
     * @throws IOException
     */
    protected void write(File out) throws IOException {
        DocumentBuilder parser;
        try {
            parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw (IOException) new IOException().initCause(e);
        }
        Document doc = parser.newDocument();

        Element root = doc.createElement(BINARY_STORE);
        doc.appendChild(root);
        root.appendChild(doc.createElement(DIGEST)).appendChild(
                doc.createTextNode(digest));
        root.appendChild(doc.createElement(DEPTH)).appendChild(
                doc.createTextNode(String.valueOf(depth)));

        try {
            Transformer trans = TransformerFactory.newInstance().newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            // don't use StreamResult(out) as it fails on paths with spaces
            Result outputTarget = new StreamResult(new FileOutputStream(out));
            trans.transform(new DOMSource(doc), outputTarget);
        } catch (TransformerException e) {
            throw (IOException) new IOException().initCause(e);
        }
    }

}
