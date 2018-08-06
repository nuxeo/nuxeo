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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.blob.binary;

import static javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING;

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
 * Descriptor for the configuration of an on-disk binaries storage.
 */
@XObject(value = BinaryManagerRootDescriptor.BINARY_STORE)
public class BinaryManagerRootDescriptor {

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
    public void write(File out) throws IOException {
        DocumentBuilder parser;
        try {
            parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw (IOException) new IOException().initCause(e);
        }
        Document doc = parser.newDocument();

        Element root = doc.createElement(BINARY_STORE);
        doc.appendChild(root);
        root.appendChild(doc.createElement(DIGEST)).appendChild(doc.createTextNode(digest));
        root.appendChild(doc.createElement(DEPTH)).appendChild(doc.createTextNode(String.valueOf(depth)));

        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(FEATURE_SECURE_PROCESSING, true);
            Transformer trans = factory.newTransformer();
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
