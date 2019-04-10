/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.diff.service.impl;

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
import org.nuxeo.ecm.core.io.DocumentXMLExporter;
import org.nuxeo.ecm.diff.model.DocumentDiff;
import org.nuxeo.ecm.diff.model.impl.DocumentDiffImpl;
import org.nuxeo.ecm.diff.service.DocumentDiffService;
import org.nuxeo.runtime.api.Framework;
import org.xml.sax.InputSource;

/**
 * Implementation of DocumentDiffService.
 * <p>
 * The diff is made by exporting the documents to XML, then using the Diff feature provided by XMLUnit to get the
 * differences between the XML exports.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class DocumentDiffServiceImpl implements DocumentDiffService {

    private static final long serialVersionUID = 9023621903602108068L;

    private static final Log LOGGER = LogFactory.getLog(DocumentDiffServiceImpl.class);

    /**
     * {@inheritDoc}
     */
    public DocumentDiff diff(CoreSession session, DocumentModel leftDoc, DocumentModel rightDoc) throws ClientException {

        // Input sources to hold XML exports
        InputSource leftDocXMLInputSource = new InputSource();
        InputSource rightDocXMLInputSource = new InputSource();

        // Export leftDoc and rightDoc to XML
        exportXML(session, leftDoc, rightDoc, leftDocXMLInputSource, rightDocXMLInputSource);

        // Process the XML diff
        DetailedDiff detailedDiff = diffXML(leftDocXMLInputSource, rightDocXMLInputSource);

        // Fill in the DocumentDiff object using the result of the detailed diff
        DocumentDiff docDiff = computeDocDiff(detailedDiff);

        return docDiff;
    }

    /**
     * {@inheritDoc}
     */
    public DocumentDiff diff(String leftXML, String rightXML) throws ClientException {

        // Process the XML diff
        DetailedDiff detailedDiff = diffXML(leftXML, rightXML);

        // Fill in the DocumentDiff object using the result of the detailed diff
        DocumentDiff docDiff = computeDocDiff(detailedDiff);

        return docDiff;
    }

    /**
     * {@inheritDoc}
     */
    public void configureXMLUnit() {

        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
        XMLUnit.setCompareUnmatched(false);
    }

    /**
     * {@inheritDoc}
     */
    public void configureDiff(Diff diff) {

        diff.overrideDifferenceListener(new IgnoreStructuralDifferenceListener());
        diff.overrideElementQualifier(new ElementNameAndAttributeQualifier());
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
    protected final void exportXML(CoreSession session, DocumentModel leftDoc, DocumentModel rightDoc,
            InputSource leftDocXMLInputSource, InputSource rightDocXMLInputSource) throws ClientException {

        DocumentXMLExporter docXMLExporter = getDocumentXMLExporter();

        leftDocXMLInputSource.setByteStream(docXMLExporter.exportXML(leftDoc, session));
        rightDocXMLInputSource.setByteStream(docXMLExporter.exportXML(rightDoc, session));
    }

    /**
     * Gets the document XML exporter service.
     *
     * @return the document XML exporter
     * @throws ClientException if the document XML exporter service cannot be found
     */
    protected final DocumentXMLExporter getDocumentXMLExporter() throws ClientException {

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
     * @param leftDocXMLInputSource the left doc XML input source
     * @param rightDocXMLInputSource the right doc XML input source
     * @return the detailed diff
     * @throws ClientException the client exception
     */
    protected final DetailedDiff diffXML(InputSource leftDocXMLInputSource, InputSource rightDocXMLInputSource)
            throws ClientException {

        DetailedDiff detailedDiff;
        try {
            // Configure XMLUnit
            configureXMLUnit();
            // Build diff
            Diff diff = new Diff(leftDocXMLInputSource, rightDocXMLInputSource);
            // Configure diff
            configureDiff(diff);
            // Build detailed diff
            detailedDiff = new DetailedDiff(diff);
        } catch (Exception e) {
            throw new ClientException("Error while trying to make a detailed diff between two documents.", e);
        }
        return detailedDiff;
    }

    /**
     * Processes the XML diff using the XMLUnit Diff feature.
     *
     * @param leftXML the left xml
     * @param rightXML the right xml
     * @return the detailed diff
     * @throws ClientException the client exception
     */
    protected final DetailedDiff diffXML(String leftXML, String rightXML) throws ClientException {

        DetailedDiff detailedDiff;
        try {
            // Configure XMLUnit
            configureXMLUnit();
            // Build diff
            Diff diff = new Diff(leftXML, rightXML);
            // Configure diff
            configureDiff(diff);
            // Build detailed diff
            detailedDiff = new DetailedDiff(diff);
        } catch (Exception e) {
            throw new ClientException("Error while trying to make a detailed diff between two XML strings.", e);
        }
        return detailedDiff;
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

                boolean fieldDiffFound = FieldDiffHelper.computeFieldDiff(docDiff, controlNodeDetail, testNodeDetail,
                        fieldDifferenceCount, difference);
                if (fieldDiffFound) {
                    fieldDifferenceCount++;
                }
            }
        }
        LOGGER.debug(String.format("Found %d field differences.", fieldDifferenceCount));

        return docDiff;
    }

}
