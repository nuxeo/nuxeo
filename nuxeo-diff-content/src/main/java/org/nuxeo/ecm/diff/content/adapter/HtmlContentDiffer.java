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

package org.nuxeo.ecm.diff.content.adapter;

import static javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.diff.content.ContentDiffException;
import org.nuxeo.ecm.diff.content.ContentDiffHelper;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.outerj.daisy.diff.HtmlCleaner;
import org.outerj.daisy.diff.XslFilter;
import org.outerj.daisy.diff.html.HTMLDiffer;
import org.outerj.daisy.diff.html.HtmlSaxDiffOutput;
import org.outerj.daisy.diff.html.TextNodeComparator;
import org.outerj.daisy.diff.html.dom.DomTreeBuilder;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * @author Antoine Taillefer
 * @since 5.6
 */
public class HtmlContentDiffer implements MimeTypeContentDiffer {

    private static final Log LOGGER = LogFactory.getLog(HtmlContentDiffer.class);

    protected static final String NUXEO_DEFAULT_CONTEXT_PATH = "/nuxeo";

    @Override
    public List<Blob> getContentDiff(DocumentModel leftDoc, DocumentModel rightDoc, String xpath, Locale locale)
            throws ContentDiffException {
        Blob leftBlob = null;
        Blob rightBlob = null;
        BlobHolder leftBlobHolder = null;
        BlobHolder rightBlobHolder = null;
        if (StringUtils.isBlank(xpath) || ContentDiffHelper.DEFAULT_XPATH.equals(xpath)) {
            leftBlobHolder = leftDoc.getAdapter(BlobHolder.class);
            rightBlobHolder = rightDoc.getAdapter(BlobHolder.class);
        } else {
            leftBlobHolder = ContentDiffHelper.getBlobHolder(leftDoc, xpath);
            rightBlobHolder = ContentDiffHelper.getBlobHolder(rightDoc, xpath);
        }
        if (leftBlobHolder == null || rightBlobHolder == null) {
            throw new ContentDiffException("Can not make a content diff of documents without a blob");
        }
        leftBlob = leftBlobHolder.getBlob();
        rightBlob = rightBlobHolder.getBlob();
        if (leftBlob == null || rightBlob == null) {
            throw new ContentDiffException("Can not make a content diff of documents without a blob");
        }
        return getContentDiff(leftBlob, rightBlob, locale);
    }

    @Override
    public List<Blob> getContentDiff(Blob leftBlob, Blob rightBlob, Locale locale) throws ContentDiffException {

        try {
            List<Blob> blobResults = new ArrayList<Blob>();
            StringWriter sw = new StringWriter();

            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(FEATURE_SECURE_PROCESSING, true);
            SAXTransformerFactory stf = (SAXTransformerFactory) factory;
            TransformerHandler transformHandler = stf.newTransformerHandler();
            transformHandler.setResult(new StreamResult(sw));

            XslFilter htmlHeaderXslFilter = new XslFilter();

            StringBuilder sb = new StringBuilder("xslfilter/htmldiffheader");
            sb.append("_");
            sb.append(locale.getLanguage());
            sb.append(".xsl");
            String htmlHeaderXslPath = sb.toString();
            ContentHandler postProcess;
            try {
                postProcess = htmlHeaderXslFilter.xsl(transformHandler, htmlHeaderXslPath);
            } catch (IllegalStateException ise) {
                LOGGER.error(String.format(
                        "Could not find the HTML diff header xsl file '%s', falling back on the default one.",
                        htmlHeaderXslPath), ise);
                postProcess = htmlHeaderXslFilter.xsl(transformHandler, "xslfilter/htmldiffheader.xsl");
            }

            String prefix = "diff";

            HtmlCleaner cleaner = new HtmlCleaner();

            InputSource leftIS = new InputSource(leftBlob.getStream());
            InputSource rightIS = new InputSource(rightBlob.getStream());

            DomTreeBuilder leftHandler = new DomTreeBuilder();
            cleaner.cleanAndParse(leftIS, leftHandler);
            TextNodeComparator leftComparator = new TextNodeComparator(leftHandler, locale);

            DomTreeBuilder rightHandler = new DomTreeBuilder();
            cleaner.cleanAndParse(rightIS, rightHandler);
            TextNodeComparator rightComparator = new TextNodeComparator(rightHandler, locale);

            postProcess.startDocument();
            postProcess.startElement("", "diffreport", "diffreport", new AttributesImpl());
            postProcess.startElement("", "diff", "diff", new AttributesImpl());
            HtmlSaxDiffOutput output = new HtmlSaxDiffOutput(postProcess, prefix);

            HTMLDiffer differ = new HTMLDiffer(output);
            differ.diff(leftComparator, rightComparator);

            postProcess.endElement("", "diff", "diff");
            postProcess.endElement("", "diffreport", "diffreport");
            postProcess.endDocument();

            String stringBlob = sw.toString().replaceAll(NUXEO_DEFAULT_CONTEXT_PATH,
                    VirtualHostHelper.getContextPathProperty());
            Blob mainBlob = Blobs.createBlob(stringBlob);
            sw.close();

            mainBlob.setFilename("contentDiff.html");
            mainBlob.setMimeType("text/html");

            blobResults.add(mainBlob);
            return blobResults;

        } catch (TransformerConfigurationException | SAXException | IOException e) {
            throw new ContentDiffException(e);
        }
    }
}
