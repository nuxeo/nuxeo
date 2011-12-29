/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ataillefer
 */
package org.nuxeo.ecm.platform.diff.service.impl;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.NodeDetail;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.diff.model.DocumentDiff;
import org.nuxeo.ecm.platform.diff.model.PropertyDiff;
import org.nuxeo.ecm.platform.diff.model.SchemaDiff;
import org.nuxeo.ecm.platform.diff.model.impl.DocumentDiffImpl;
import org.nuxeo.ecm.platform.diff.model.impl.SimplePropertyDiff;
import org.nuxeo.ecm.platform.diff.service.DocumentDiffService;
import org.nuxeo.ecm.platform.xmlexport.DocumentXMLExporter;
import org.nuxeo.runtime.api.Framework;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * Implementation of DocumentDiffService.
 * <p>
 * The diff is made by exporting the documents to XML, then using the Diff
 * feature provided by XMLUnit to get the differences between the XML exports.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class DocumentDiffServiceImpl implements DocumentDiffService {

    private static final long serialVersionUID = 9023621903602108068L;

    private static final Log LOGGER = LogFactory.getLog(DocumentDiffServiceImpl.class);

    private static final String SCHEMA_ELEMENT = "schema";

    private static final String NAME_ATTRIBUTE = "name";

    /**
     * {@inheritDoc}
     */
    public DocumentDiff diff(CoreSession session, DocumentModel leftDoc,
            DocumentModel rightDoc) throws ClientException {

        // Export leftDoc and rightDoc to XML
        InputSource leftDocXMLInputSource = new InputSource();
        InputSource rightDocXMLInputSource = new InputSource();
        exportXML(session, leftDoc, rightDoc, leftDocXMLInputSource,
                rightDocXMLInputSource);

        // Process the XML diff
        DetailedDiff detailedDiff = diffXML(leftDocXMLInputSource,
                rightDocXMLInputSource, leftDoc.getTitle(), rightDoc.getTitle());

        // Fill in the DocumentDiff object using the result of the detailed diff
        DocumentDiff docDiff = computeDocDiff(detailedDiff);

        return docDiff;
    }

    /**
     * Exports leftDoc and rightDoc to XML.
     * 
     * @param session the session
     * @param leftDoc the left doc
     * @param rightDoc the right doc
     * @param leftDocXMLInputSource the left doc xml input source
     * @param rightDocXMLInputSource the right doc xml input source
     * @throws ClientException the client exception
     */
    protected final void exportXML(CoreSession session, DocumentModel leftDoc,
            DocumentModel rightDoc, InputSource leftDocXMLInputSource,
            InputSource rightDocXMLInputSource) throws ClientException {

        DocumentXMLExporter docXMLExporter = getDocumentXMLExporter();

        leftDocXMLInputSource.setByteStream(docXMLExporter.exportXML(leftDoc,
                session));
        rightDocXMLInputSource.setByteStream(docXMLExporter.exportXML(rightDoc,
                session));
    }

    /**
     * Gets the document XML exporter service.
     * 
     * @return the document XML exporter
     * @throws ClientException the client exception
     */
    protected final DocumentXMLExporter getDocumentXMLExporter()
            throws ClientException {

        DocumentXMLExporter docXMLExporter;

        try {
            docXMLExporter = Framework.getService(DocumentXMLExporter.class);
        } catch (Exception e) {
            throw ClientException.wrap(e);
        }
        if (docXMLExporter == null) {
            throw new ClientException("DocumentXMLExporter service is null.");
        }
        return docXMLExporter;
    }

    /**
     * Processes the XML diff using the XMLUnit Diff feature.
     * 
     * @param leftDocXMLInputSource the left doc xml input source
     * @param rightDocXMLInputSource the right doc xml input source
     * @param leftDocTitle the left doc title
     * @param rightDocTitle the right doc title
     * @return the detailed diff
     * @throws ClientException the client exception
     */
    protected final DetailedDiff diffXML(InputSource leftDocXMLInputSource,
            InputSource rightDocXMLInputSource, String leftDocTitle,
            String rightDocTitle) throws ClientException {

        DetailedDiff detailedDiff;
        try {
            Diff diff = new Diff(leftDocXMLInputSource, rightDocXMLInputSource);
            detailedDiff = new DetailedDiff(diff);
        } catch (Exception e) {
            throw new ClientException(
                    String.format(
                            "Error while trying to make a detailed diff between the documents %s and %s.",
                            leftDocTitle, rightDocTitle), e);
        }
        return detailedDiff;
    }

    /**
     * Computes the doc diff.
     * 
     * @param detailedDiff the detailed diff
     * @return the document diff
     */
    @SuppressWarnings("unchecked")
    protected DocumentDiff computeDocDiff(DetailedDiff detailedDiff) {

        // Document diff object
        DocumentDiff docDiff = new DocumentDiffImpl();

        // Iterate on differences
        List<Difference> differences = detailedDiff.getAllDifferences();
        for (Difference difference : differences) {

            // Control node <=> left doc node
            NodeDetail controlNodeDetail = difference.getControlNodeDetail();
            // Test node <=> right doc node
            NodeDetail testNodeDetail = difference.getTestNodeDetail();

            if (controlNodeDetail != null && testNodeDetail != null) {

                // Use control node to detect schema and field elements
                Node controlNode = controlNodeDetail.getNode();
                if (controlNode != null) {

                    String field = null;

                    // Detect a schema element.
                    // For instance : <schema name="dublincore" xmlns:dc="...">
                    Node currentNode = controlNode;
                    Node parentNode = controlNode.getParentNode();
                    while (parentNode != null
                            && !SCHEMA_ELEMENT.equals(currentNode.getNodeName())) {

                        // Detect a field element, ie. an element that has a
                        // prefix.
                        // For instance: <dc:title> (prefix = "dc")
                        if (parentNode.getPrefix() != null) {
                            field = parentNode.getLocalName();
                        }

                        currentNode = parentNode;
                        parentNode = parentNode.getParentNode();
                    }

                    // If we found a schema element (ie. we did not reached the
                    // root element, ie. parentNode != null) and a nested field
                    // element, we can compute the diff for this field.
                    if (parentNode != null && field != null) {
                        NamedNodeMap attr = currentNode.getAttributes();
                        if (attr != null && attr.getLength() > 0) {
                            Node nameAttr = attr.getNamedItem(NAME_ATTRIBUTE);
                            if (nameAttr != null) {
                                String schema = nameAttr.getNodeValue();
                                LOGGER.info(String.format(
                                        "Found a difference on field %s of schema %s. Will compute field diff.",
                                        field, schema));

                                computeFieldDiff(docDiff, schema, field,
                                        controlNodeDetail, testNodeDetail);
                            }
                        }
                    }
                }
            }
        }
        return docDiff;
    }

    /**
     * Compute field diff.
     * 
     * @param docDiff the doc diff
     * @param schema the schema
     * @param field the field
     * @param controlNodeDetail the control node detail
     * @param testNodeDetail the test node detail
     */
    protected final void computeFieldDiff(DocumentDiff docDiff, String schema,
            String field, NodeDetail controlNodeDetail,
            NodeDetail testNodeDetail) {

        SchemaDiff schemaDiff = docDiff.getSchemaDiff(schema);
        if (schemaDiff == null) {
            schemaDiff = docDiff.initSchemaDiff(schema);
        }

        PropertyDiff propertyDiff = schemaDiff.getFieldDiff(field);

        // XXX:ATA Manage complex types here
        if (propertyDiff == null) {
            PropertyDiff fieldDiff = new SimplePropertyDiff(
                    controlNodeDetail.getValue(), testNodeDetail.getValue());
            schemaDiff.putFieldDiff(field, fieldDiff);
        }
    }

}
