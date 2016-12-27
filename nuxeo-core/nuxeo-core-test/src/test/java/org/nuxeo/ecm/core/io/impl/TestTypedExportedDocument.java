/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     ataillefer
 */

package org.nuxeo.ecm.core.io.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import javax.inject.Inject;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.dom4j.io.DocumentSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Tests TypedExportedDocument.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = TypedExportedDocumentRepositoryInit.class, cleanup = Granularity.METHOD)
public class TestTypedExportedDocument {

    /** The Constant transformerFactory. */
    protected static final TransformerFactory transformerFactory = TransformerFactory.newInstance();

    /** The session. */
    @Inject
    protected CoreSession session;

    /**
     * Test typed exported document.
     *
     * @throws Exception the exception
     */
    @Test
    public void testTypedExportedDocument() throws Exception {

        DocumentModel doc = session.getDocument(new PathRef("/" + TypedExportedDocumentRepositoryInit.TEST_DOC_NAME));
        ExportedDocument exportedDoc = new TypedExportedDocumentImpl(doc);

        // Check system elements.
        assertEquals("File", exportedDoc.getType());
        assertEquals("testDoc", exportedDoc.getPath().toString());

        // Get w3c Document
        org.dom4j.Document dom4jDocument = exportedDoc.getDocument();
        Document document = dom4jToW3c(dom4jDocument);

        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new CoreNamespaceContext());

        // Check dublincore schema
        Node schemaNode = (Node) xpath.evaluate("//schema[@name='dublincore']", document, XPathConstants.NODE);
        assertNotNull(schemaNode);

        Node fieldNode = (Node) xpath.evaluate("//dc:title[@type='string']", document, XPathConstants.NODE);
        assertNotNull(fieldNode);
        assertEquals("My test doc", fieldNode.getTextContent());

        fieldNode = (Node) xpath.evaluate("//dc:created[@type='date']", document, XPathConstants.NODE);
        assertNotNull(fieldNode);
        assertEquals("2011-12-29T11:24:25.00Z", fieldNode.getTextContent());

        fieldNode = (Node) xpath.evaluate("//dc:creator[@type='string']", document, XPathConstants.NODE);
        assertNotNull(fieldNode);
        assertEquals("Administrator", fieldNode.getTextContent());

        fieldNode = (Node) xpath.evaluate("//dc:modified[@type='date']", document, XPathConstants.NODE);
        assertNotNull(fieldNode);
        assertEquals("2011-12-29T11:24:25.00Z", fieldNode.getTextContent());

        fieldNode = (Node) xpath.evaluate("//dc:lastContributor[@type='string']", document, XPathConstants.NODE);
        assertNotNull(fieldNode);
        assertEquals("Administrator", fieldNode.getTextContent());

        fieldNode = (Node) xpath.evaluate("//dc:contributors[@type='scalarList']", document, XPathConstants.NODE);
        assertNotNull(fieldNode);

        fieldNode = (Node) xpath.evaluate("//dc:contributors[@type='scalarList']/item[1]", document,
                XPathConstants.NODE);
        assertNotNull(fieldNode);
        assertEquals("Administrator", fieldNode.getTextContent());

        fieldNode = (Node) xpath.evaluate("//dc:contributors[@type='scalarList']/item[2]", document,
                XPathConstants.NODE);
        assertNotNull(fieldNode);
        assertEquals("Joe", fieldNode.getTextContent());

        fieldNode = (Node) xpath.evaluate("//dc:subjects[@type='scalarList']", document, XPathConstants.NODE);
        assertNotNull(fieldNode);

        fieldNode = (Node) xpath.evaluate("//dc:subjects[@type='scalarList']/item[1]", document, XPathConstants.NODE);
        assertNotNull(fieldNode);
        assertEquals("Art", fieldNode.getTextContent());

        fieldNode = (Node) xpath.evaluate("//dc:subjects[@type='scalarList']/item[2]", document, XPathConstants.NODE);
        assertNotNull(fieldNode);
        assertEquals("Architecture", fieldNode.getTextContent());

        // Check file schema
        schemaNode = (Node) xpath.evaluate("//schema[@name='file']", document, XPathConstants.NODE);
        assertNotNull(schemaNode);

        fieldNode = (Node) xpath.evaluate("//file:content[@type='content']", document, XPathConstants.NODE);
        assertNotNull(fieldNode);

        fieldNode = (Node) xpath.evaluate("//file:content[@type='content']/encoding", document, XPathConstants.NODE);
        assertNotNull(fieldNode);
        assertEquals("UTF-8", fieldNode.getTextContent());

        fieldNode = (Node) xpath.evaluate("//file:content[@type='content']/mime-type", document, XPathConstants.NODE);
        assertNotNull(fieldNode);
        assertEquals("text/plain", fieldNode.getTextContent());

        fieldNode = (Node) xpath.evaluate("//file:content[@type='content']/filename", document, XPathConstants.NODE);
        assertNotNull(fieldNode);
        assertEquals("test_file.doc", fieldNode.getTextContent());

        fieldNode = (Node) xpath.evaluate("//file:content[@type='content']/data", document, XPathConstants.NODE);
        assertNotNull(fieldNode);
        assertTrue(!StringUtils.isEmpty(fieldNode.getTextContent()));

        fieldNode = (Node) xpath.evaluate("//file:content[@type='content']/digest", document, XPathConstants.NODE);
        assertNotNull(fieldNode);
        assertTrue(!StringUtils.isEmpty(fieldNode.getTextContent()));
    }

    /**
     * Transforms a dom4j document to a w3c Document.
     *
     * @param dom4jdoc the org.dom4j.Document document
     * @return the org.w3c.dom.Document document
     * @throws TransformerException the transformer exception
     */
    protected final Document dom4jToW3c(org.dom4j.Document dom4jdoc) throws TransformerException {

        SAXSource source = new DocumentSource(dom4jdoc);
        DOMResult result = new DOMResult();

        Transformer transformer = transformerFactory.newTransformer();
        transformer.transform(source, result);

        return (Document) result.getNode();
    }

    /**
     * The NamespaceContext for Nuxeo basic core schemas.
     */
    @SuppressWarnings("rawtypes")
    protected final class CoreNamespaceContext implements NamespaceContext {

        @Override
        public String getNamespaceURI(String prefix) {

            if ("dc".equals(prefix)) {
                return "http://www.nuxeo.org/ecm/schemas/dublincore/";
            } else if ("file".equals(prefix)) {
                return "http://www.nuxeo.org/ecm/schemas/file/";
            } else {
                return XMLConstants.NULL_NS_URI;
            }
        }

        // Unused => dummy
        @Override
        public String getPrefix(String namespace) {
            return null;
        }

        // Unused => dummy
        @Override
        public Iterator getPrefixes(String namespace) {
            return null;
        }
    }

}
