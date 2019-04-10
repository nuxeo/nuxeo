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
package org.nuxeo.ecm.diff.service.impl;

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.ElementNameAndAttributeQualifier;
import org.custommonkey.xmlunit.NodeDetail;
import org.custommonkey.xmlunit.XMLUnit;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.io.DocumentXMLExporter;
import org.nuxeo.ecm.diff.model.DocumentDiff;
import org.nuxeo.ecm.diff.model.impl.DocumentDiffImpl;
import org.nuxeo.ecm.diff.service.DocumentDiffService;
import org.nuxeo.runtime.api.Framework;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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
    public DocumentDiff diff(CoreSession session, DocumentModel leftDoc, DocumentModel rightDoc) {

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
    public DocumentDiff diff(String leftXML, String rightXML) {

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
     */
    protected final void exportXML(CoreSession session, DocumentModel leftDoc, DocumentModel rightDoc,
            InputSource leftDocXMLInputSource, InputSource rightDocXMLInputSource) {

        DocumentXMLExporter docXMLExporter = getDocumentXMLExporter();

        leftDocXMLInputSource.setByteStream(docXMLExporter.exportXML(leftDoc, session));
        rightDocXMLInputSource.setByteStream(docXMLExporter.exportXML(rightDoc, session));
    }

    /**
     * Gets the document XML exporter service.
     *
     * @return the document XML exporter
     */
    protected final DocumentXMLExporter getDocumentXMLExporter() {
        return Framework.getService(DocumentXMLExporter.class);
    }

    /**
     * Processes the XML diff using the XMLUnit Diff feature.
     *
     * @param leftDocXMLInputSource the left doc XML input source
     * @param rightDocXMLInputSource the right doc XML input source
     * @return the detailed diff
     */
    protected final DetailedDiff diffXML(InputSource leftDocXMLInputSource, InputSource rightDocXMLInputSource)
            {

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
        } catch (SAXException | IOException e) {
            throw new NuxeoException("Error while trying to make a detailed diff between two documents.", e);
        }
        return detailedDiff;
    }

    /**
     * Processes the XML diff using the XMLUnit Diff feature.
     *
     * @param leftXML the left xml
     * @param rightXML the right xml
     * @return the detailed diff
     */
    protected final DetailedDiff diffXML(String leftXML, String rightXML) {

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
        } catch (SAXException | IOException e) {
            throw new NuxeoException("Error while trying to make a detailed diff between two XML strings.", e);
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
    protected final DocumentDiff computeDocDiff(DetailedDiff detailedDiff) {

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
