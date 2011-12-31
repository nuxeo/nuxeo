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

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.ElementNameAndAttributeQualifier;
import org.custommonkey.xmlunit.NodeDetail;
import org.custommonkey.xmlunit.XMLUnit;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.diff.model.DocumentDiff;
import org.nuxeo.ecm.platform.diff.model.PropertyDiffType;
import org.nuxeo.ecm.platform.diff.model.impl.DocumentDiffImpl;
import org.nuxeo.ecm.platform.diff.service.DocumentDiffService;
import org.nuxeo.ecm.platform.xmlexport.DocumentXMLExporter;
import org.nuxeo.runtime.api.Framework;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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

    private static final String SYSTEM_ELEMENT = "system";

    private static final String TYPE_ELEMENT = "type";

    private static final String PATH_ELEMENT = "path";

    private static final String[] SYSTEM_ELEMENTS = { TYPE_ELEMENT,
            PATH_ELEMENT };

    private static final String SCHEMA_ELEMENT = "schema";

    private static final String NAME_ATTRIBUTE = "name";

    /**
     * {@inheritDoc}
     */
    public DocumentDiff diff(CoreSession session, DocumentModel leftDoc,
            DocumentModel rightDoc) throws ClientException {

        // Input sources to hold XML exports
        InputSource leftDocXMLInputSource = new InputSource();
        InputSource rightDocXMLInputSource = new InputSource();

        // Export leftDoc and rightDoc to XML
        exportXML(session, leftDoc, rightDoc, leftDocXMLInputSource,
                rightDocXMLInputSource);

        // Process the XML diff
        DetailedDiff detailedDiff = diffXML(leftDocXMLInputSource,
                rightDocXMLInputSource);

        // Fill in the DocumentDiff object using the result of the detailed diff
        DocumentDiff docDiff = computeDocDiff(detailedDiff);

        return docDiff;
    }

    /**
     * {@inheritDoc}
     */
    public DocumentXMLExporter getDocumentXMLExporter() throws ClientException {

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
     * Exports leftDoc and rightDoc to XML.
     * 
     * @param session the session
     * @param leftDoc the left doc
     * @param rightDoc the right doc
     * @param leftDocXMLInputSource the left doc XML input source
     * @param rightDocXMLInputSource the right doc XML input source
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
     * Processes the XML diff using the XMLUnit Diff feature.
     * 
     * @param leftDocXMLInputSource the left doc XML input source
     * @param rightDocXMLInputSource the right doc XML input source
     * @return the detailed diff
     * @throws ClientException the client exception
     */
    protected final DetailedDiff diffXML(InputSource leftDocXMLInputSource,
            InputSource rightDocXMLInputSource) throws ClientException {

        DetailedDiff detailedDiff;
        try {
            configureXMLUnit();
            Diff diff = new Diff(leftDocXMLInputSource, rightDocXMLInputSource);
            configureDiff(diff);
            detailedDiff = new DetailedDiff(diff);
        } catch (Exception e) {
            throw new ClientException(
                    "Error while trying to make a detailed diff between two documents.",
                    e);
        }
        return detailedDiff;
    }

    /**
     * Configures XMLUnit.
     */
    protected void configureXMLUnit() {

        // XXX ATA: contribute this configuration to the service
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
    }

    /**
     * Configures the diff.
     * 
     * @param diff the diff
     */
    protected void configureDiff(Diff diff) {

        // XXX ATA: contribute this configuration to the service
        diff.overrideElementQualifier(new ElementNameAndAttributeQualifier());
        diff.overrideDifferenceListener(new IgnoreStructuralDifferenceListener());
    }

    /**
     * Computes the doc diff.
     *
     * @param detailedDiff the detailed diff
     * @return the document diff
     * @throws ClientException the client exception
     */
    @SuppressWarnings("unchecked")
    protected final DocumentDiff computeDocDiff(DetailedDiff detailedDiff) throws ClientException {

        // Document diff object
        DocumentDiff docDiff = new DocumentDiffImpl();

        // Iterate on differences
        List<Difference> differences = detailedDiff.getAllDifferences();
        LOGGER.debug(String.format("Found %d differences.", differences.size()));

        int fieldDifferenceCount = 0;
        for (Difference difference : differences) {

            // Control node <=> left doc node
            NodeDetail controlNodeDetail = difference.getControlNodeDetail();
            // Test node <=> right doc node
            NodeDetail testNodeDetail = difference.getTestNodeDetail();

            if (controlNodeDetail != null && testNodeDetail != null) {

                // Use control node or if null test node to detect schema and
                // field elements
                Node currentNode = controlNodeDetail.getNode();
                if (currentNode == null) {
                    currentNode = testNodeDetail.getNode();
                }
                if (currentNode != null) {

                    String currentNodeName = currentNode.getNodeName();
                    String field = null;
                    PropertyDiffType propertyDiffType = PropertyDiffType.simple;

                    // Detect a schema element,
                    // for instance: <schema name="dublincore" xmlns:dc="...">,
                    // or the <system> element.
                    Node parentNode = currentNode.getParentNode();
                    while (parentNode != null
                            && !SCHEMA_ELEMENT.equals(currentNodeName)
                            && !SYSTEM_ELEMENT.equals(currentNodeName)) {

                        // Detect a field element, ie. an element that has a
                        // prefix, for instance: <dc:title>,
                        // or an element nested in <system>.
                        if (currentNode.getPrefix() != null
                                || Arrays.asList(SYSTEM_ELEMENTS).contains(
                                        currentNode.getNodeName())) {
                            field = currentNode.getLocalName();
                            propertyDiffType = getPropertyDiffType(currentNode);

                        }
                        currentNode = parentNode;
                        currentNodeName = currentNode.getNodeName();
                        parentNode = parentNode.getParentNode();
                    }

                    // If we found a schema or system element (ie. we did not
                    // reached the root element, ie. parentNode != null) and a
                    // nested field element, we can compute the diff for this
                    // field.
                    if (parentNode != null && field != null) {
                        String schema = currentNodeName;
                        // Get schema name if not system
                        if (!SYSTEM_ELEMENT.equals(schema)) {
                            NamedNodeMap attr = currentNode.getAttributes();
                            if (attr != null && attr.getLength() > 0) {
                                Node nameAttr = attr.getNamedItem(NAME_ATTRIBUTE);
                                if (nameAttr != null) {
                                    schema = nameAttr.getNodeValue();
                                }
                            }
                        }

                        // Increment field differences count and pretty
                        // log field difference
                        fieldDifferenceCount++;
                        LOGGER.info(String.format(
                                "Found field difference #%d on [%s]/[%s] (%s): [%s (%s)] {%s --> %s}",
                                fieldDifferenceCount, schema, field,
                                propertyDiffType.name(),
                                difference.getDescription(),
                                difference.getId(),
                                controlNodeDetail.getValue(),
                                testNodeDetail.getValue()));

                        // Compute field diff
                        FieldDiffHelper.computeFieldDiff(docDiff, schema,
                                field, propertyDiffType, difference.getId(),
                                controlNodeDetail, testNodeDetail);
                    } else {// Non-field difference
                        LOGGER.debug(String.format(
                                "Found non-field difference: [%s (%s)] {%s --> %s}",
                                difference.getDescription(),
                                difference.getId(),
                                controlNodeDetail.getValue(),
                                testNodeDetail.getValue()));
                    }
                }
            }
        }
        return docDiff;
    }

    /**
     * Gets the property diff type.
     * 
     * @param node the node
     * @return the property diff type
     */
    protected final PropertyDiffType getPropertyDiffType(Node node) {

        // Default: simple type
        PropertyDiffType propertyDiffType = PropertyDiffType.simple;

        NodeList childNodes = node.getChildNodes();
        int childNodesLength = childNodes.getLength();
        if (childNodesLength > 1) {

            // At least 2 child nodes => list type
            propertyDiffType = PropertyDiffType.list;

            for (int i = 1; i < childNodes.getLength(); i++) {
                String firstChildNodeName = childNodes.item(0).getNodeName();
                if (!firstChildNodeName.equals(childNodes.item(i).getNodeName())) {

                    // All child nodes don't have the same name => complex type
                    propertyDiffType = PropertyDiffType.complex;
                    break;
                }
            }
        }

        return propertyDiffType;
    }

}
