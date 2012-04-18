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
 */
package org.nuxeo.ecm.diff.detaileddiff.adapter.base;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentStringBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterNotAvailable;
import org.nuxeo.ecm.diff.detaileddiff.DetailedDiffException;
import org.nuxeo.ecm.diff.detaileddiff.adapter.HtmlDetailedDiffer;
import org.nuxeo.ecm.diff.detaileddiff.adapter.MimeTypeDetailedDiffer;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;

/**
 * Base class for detailed diff based on "on the fly html or text transformers.
 *
 * @author Antoine Taillefer
 * @since 5.6
 */
public class ConverterBasedDetailedDiffAdapter extends
        AbstractDetailedDiffAdapter {

    private static final Log log = LogFactory.getLog(ConverterBasedDetailedDiffAdapter.class);

    protected String defaultFieldXPath;

    protected MimetypeRegistry mimeTypeService;

    @Override
    public List<Blob> getDetailedDiffBlobs(DocumentModel otherDoc,
            DetailedDiffConversionType conversionType)
            throws DetailedDiffException {
        return getDetailedDiffBlobs(otherDoc,
                getDefaultDetailedDiffFieldXPath(), conversionType);
    }

    @Override
    public List<Blob> getDetailedDiffBlobs(DocumentModel otherDoc,
            String xpath, DetailedDiffConversionType conversionType)
            throws DetailedDiffException {

        Blob adaptedDocBlob = null;
        Blob otherDocBlob = null;
        BlobHolder adaptedDocBlobHolder = null;
        BlobHolder otherDocBlobHolder = null;

        try {
            if ((xpath == null) || ("default".equals(xpath))) {
                adaptedDocBlobHolder = adaptedDoc.getAdapter(BlobHolder.class);
                otherDocBlobHolder = otherDoc.getAdapter(BlobHolder.class);
            } else {
                adaptedDocBlobHolder = getBlobHolder(adaptedDoc, xpath);
                otherDocBlobHolder = getBlobHolder(otherDoc, xpath);
            }
            if (adaptedDocBlobHolder == null || otherDocBlobHolder == null) {
                throw new DetailedDiffException(
                        "Can not make a detailed diff of documents without a blob");
            }

            adaptedDocBlob = adaptedDocBlobHolder.getBlob();
            otherDocBlob = otherDocBlobHolder.getBlob();
            if (adaptedDocBlob == null || otherDocBlob == null) {
                throw new DetailedDiffException(
                        "Can not make a detailed diff of documents without a blob");
            }
        } catch (ClientException e) {
            throw new DetailedDiffException("Error while getting blobs", e);
        }

        List<Blob> blobResults = new ArrayList<Blob>();

        String adaptedDocMimeType = getMimeType(adaptedDocBlob);
        String otherDocMimeType = getMimeType(otherDocBlob);
        log.debug("Mime type of adapted doc for HTML detailed diff = "
                + adaptedDocMimeType);
        log.debug("Mime type of other doc for HTML detailed diff = "
                + otherDocMimeType);

        // TODO: if conversionType == "text/plain", use TextDetailedDiffer?

        // Check doc mime types, if a common mime type is found, look for the
        // associated detailed differ.
        if (adaptedDocMimeType != null && otherDocMimeType != null
                && adaptedDocMimeType.equals(otherDocMimeType)) {
            MimeTypeDetailedDiffer mtDetailedDiffer = getDetailedDiffAdapterManager().getDetailedDiffer(
                    adaptedDocMimeType);
            if (mtDetailedDiffer != null) {
                blobResults = mtDetailedDiffer.getDetailedDiff(adaptedDocBlob,
                        otherDocBlob, adaptedDoc, otherDoc);
                return blobResults;
            }
        }

        // Docs have a different mime type or no detailed differ found for the
        // common mime type.
        // Fall back on a Html conversion + HtmlDetailedDiffer.
        String destMimeType = conversionType.getValue();
        Blob adaptedDocHtmlConvertedBlob = getHtmlConvertedBlob(
                adaptedDocMimeType, destMimeType, adaptedDocBlobHolder);
        Blob otherDocHtmlConvertedBlob = getHtmlConvertedBlob(otherDocMimeType,
                destMimeType, otherDocBlobHolder);

        HtmlDetailedDiffer htmlDetailedDiffer = getDetailedDiffAdapterManager().getHtmlDetailedDiffer();
        blobResults = htmlDetailedDiffer.getDetailedDiff(
                adaptedDocHtmlConvertedBlob, otherDocHtmlConvertedBlob,
                adaptedDoc, otherDoc);
        return blobResults;
    }

    public void cleanup() {
        // Nothing to do here
    }

    public boolean cachable() {
        return true;
    }

    public void setDefaultDetailedDiffFieldXPath(String xPath) {
        defaultFieldXPath = xPath;
    }

    protected BlobHolder getBlobHolder(DocumentModel doc, String xPath)
            throws ClientException {
        // TODO: manage other property types than Blob / String?
        Serializable prop = doc.getPropertyValue(xPath);
        if (prop instanceof Blob) {
            return new DocumentBlobHolder(doc, xPath);
        }
        if (prop instanceof String) {
            // TODO: use HTML guesser to find out mime type
            // (and don't use html for text notes)
            String mimeType = "text/plain";
            if ("note:note".equals(xPath)) {
                mimeType = (String) doc.getPropertyValue("note:mime_type");
            }
            return new DocumentStringBlobHolder(doc, xPath, mimeType);
        }
        throw new ClientException(String.format(
                "Cannot get BlobHolder for doc '%s' and xpath '%s'.",
                doc.getTitle(), xPath));
    }

    protected String getMimeType(Blob blob) {
        if (blob == null) {
            return null;
        }

        String srcMT = blob.getMimeType();
        if (srcMT == null || srcMT.startsWith("application/octet-stream")) {
            // call MT Service
            try {
                MimetypeRegistry mtr = Framework.getService(MimetypeRegistry.class);
                srcMT = mtr.getMimetypeFromFilenameAndBlobWithDefault(
                        blob.getFilename(), blob, "application/octet-stream");
                log.debug("mime type service returned " + srcMT);
            } catch (Exception e) {
                log.warn("error while calling Mimetype service", e);
            }
        }
        return srcMT;
    }

    protected void setMimeType(BlobHolder result) throws ClientException {
        for (Blob blob : result.getBlobs()) {
            if (blob.getMimeType() == null
                    && blob.getFilename().endsWith("html")) {
                String mimeTpye = getMimeType(blob);
                blob.setMimeType(mimeTpye);
            }
        }
    }

    protected String getDefaultDetailedDiffFieldXPath() {
        return defaultFieldXPath;
    }

    protected Blob getHtmlConvertedBlob(String srcMimeType,
            String destMimeType, BlobHolder blobHolder)
            throws DetailedDiffException {

        // TODO: manage converter name with a parameter (any2html / office2html
        // / ...)
        String converterName = null;
        try {
            converterName = getConversionService().getConverterName(
                    srcMimeType, destMimeType);
        } catch (Exception e) {
            throw new DetailedDiffException("Unable to get converter", e);
        }

        if (converterName == null) {
            log.debug("No dedicated converter found, using generic one: any2html.");
            converterName = "any2html";
        }

        // TODO: remove hack!
        if ("any2html".equals(converterName)) {
            converterName = "office2html";
        }

        BlobHolder convertedBlobHolder;
        try {
            convertedBlobHolder = getConversionService().convert(converterName,
                    blobHolder, null);
            setMimeType(convertedBlobHolder);
            return convertedBlobHolder.getBlob();
        } catch (ConverterNotAvailable e) {
            throw new DetailedDiffException(e.getMessage(), e);
        } catch (ConversionException e) {
            throw new DetailedDiffException("Error during conversion", e);
        } catch (Exception e) {
            throw new DetailedDiffException("Unexpected Error", e);
        }
    }

    /**
     * Gets the conversion service.
     *
     * @return the conversion service
     * @throws ClientException the client exception
     */
    protected final ConversionService getConversionService()
            throws ClientException {

        ConversionService conversionService;
        try {
            conversionService = Framework.getService(ConversionService.class);
        } catch (Exception e) {
            throw ClientException.wrap(e);
        }
        if (conversionService == null) {
            throw new ClientException("ConversionService service is null.");
        }
        return conversionService;
    }

}
