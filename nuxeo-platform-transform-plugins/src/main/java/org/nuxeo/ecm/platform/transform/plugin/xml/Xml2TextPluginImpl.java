/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     dragos
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.transform.plugin.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.DOMWriter;
import org.dom4j.io.SAXReader;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.plugin.AbstractPlugin;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

/**
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 *
 */
public class Xml2TextPluginImpl extends AbstractPlugin {

    private static final long serialVersionUID = -8395742119156169742L;

    public List<TransformDocument> transform(Map<String, Serializable> options,
            TransformDocument... sources) throws Exception {
        final List<TransformDocument> results = super.transform(options,
                sources);

        extractFromXml(results, sources);

        return results;
    }

    public void extractFromXml(List<TransformDocument> results,
            TransformDocument... sources) throws Exception {

        for (final TransformDocument source : sources) {
            final InputStream sourceIs = source.getBlob().getStream();
            final Blob result = extractFromXmlSource(sourceIs);
            results.add(new TransformDocumentImpl(result, result.getMimeType()));
        }
    }

    /**
     * @param results
     * @param sourceIs
     * @return
     * @throws DocumentException
     * @throws Exception
     * @throws IOException
     */
    private Blob extractFromXmlSource(final InputStream sourceIs)
            throws DocumentException, Exception, IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        SAXReader reader = new SAXReader();
        reader.setMergeAdjacentText(true);
        reader.setFeature("http://xml.org/sax/features/validation", false);
        reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);

        Document dom4jDoc = reader.read(sourceIs);

        // use DOMWriter to convert the document to a w3c.Document
        DOMWriter writer = new DOMWriter();

        org.w3c.dom.Document domDoc = writer.write(dom4jDoc);

        // 2nd option:
        // DocumentBuilderFactory factory =
        // DocumentBuilderFactory.newInstance();
        // DocumentBuilder builder = factory.newDocumentBuilder();
        // InputSource is = new InputSource(new StringReader(source));
        // domDoc = builder.parse(is);

        // create an instance of a xerces TreeWalker

        Writer outWriter = new OutputStreamWriter(out);
        extractTextFromDocument(domDoc, outWriter);
        outWriter.close();

        // return result
        final InputStream in = new ByteArrayInputStream(out.toByteArray());

        final Blob result = new FileBlob(in, "text/plain");
        return result;
    }

    public void extractTextFromDocument(org.w3c.dom.Document srcDoc,
            Writer writer) throws Exception {
        AllElementsFilter allelements = new AllElementsFilter();
        Node sourceRoot = srcDoc.getLastChild();
        DocumentTraversal sourceImpl = (DocumentTraversal) srcDoc;
        TreeWalker tw = (TreeWalker) sourceImpl.createTreeWalker(sourceRoot,
                NodeFilter.SHOW_ALL, allelements, true);
        walk(tw, writer);
    }

    private void walk(TreeWalker sourceIterator, Writer writer)
            throws DOMException, IOException {
        Node n = sourceIterator.getCurrentNode();
        for (Node tagSourceElem = sourceIterator.firstChild(); tagSourceElem != null; tagSourceElem = sourceIterator.nextSibling()) {
            if (tagSourceElem.getNodeValue() != null) {

                // System.out.println("The name and value of the node: "
                // + tagSourceElem.getNodeName() + " "
                // + tagSourceElem.getNodeValue());
                writer.write(tagSourceElem.getNodeValue() + " ");
                System.out.println(tagSourceElem.getNodeValue());
            }
            walk(sourceIterator, writer);
        }
        sourceIterator.setCurrentNode(n);
    }

    /**
     * filters the elements of the XML document
     */
    class AllElementsFilter implements NodeFilter {
        public short acceptNode(Node n) {
            if (n.getNodeType() > 0)
                return FILTER_ACCEPT;
            return FILTER_SKIP;
        }
    }

    /*
     * test local
     */
    public static void main(String[] args) {
        Xml2TextPluginImpl inst = new Xml2TextPluginImpl();

        String source = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<body><p>This Document is for testing ]: Axe</p>joe<p>t &quot; is a&quot;.  t &quot;work&quot;. &quot;Ana&quot; are mere.  Kaine .</p></body>";

        ByteArrayInputStream bais = new ByteArrayInputStream(source.getBytes());
        // InputSource is = new InputSource(new StringReader(source));

        try {
            Blob blob = inst.extractFromXmlSource(bais);
            System.out.println("ext   " + blob.getByteArray().length);
            System.out.println("ext text: " + new String(blob.getByteArray()));
        } catch (DocumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
