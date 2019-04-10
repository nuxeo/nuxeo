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
package org.nuxeo.ecm.diff.content.adapter.base;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentStringBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.diff.content.ContentDiffException;
import org.nuxeo.ecm.diff.content.HtmlGuesser;
import org.nuxeo.ecm.diff.content.adapter.HtmlContentDiffer;
import org.nuxeo.ecm.diff.content.adapter.MimeTypeContentDiffer;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;

/**
 * Base class for content diff based on "on the fly html or text transformers.
 *
 * @author Antoine Taillefer
 * @since 5.6
 */
public class ConverterBasedContentDiffAdapter extends AbstractContentDiffAdapter {

    private static final Log log = LogFactory.getLog(ConverterBasedContentDiffAdapter.class);

    protected static final String DEFAULT_CONVERTER_NAME = "any2text";

    protected String defaultFieldXPath;

    protected MimetypeRegistry mimeTypeService;

    @Override
    public List<Blob> getContentDiffBlobs(DocumentModel otherDoc, ContentDiffConversionType conversionType,
            Locale locale) throws ContentDiffException, ConversionException {
        return getContentDiffBlobs(otherDoc, getDefaultContentDiffFieldXPath(), conversionType, locale);
    }

    @Override
    public List<Blob> getContentDiffBlobs(DocumentModel otherDoc, String xpath,
            ContentDiffConversionType conversionType, Locale locale) throws ContentDiffException, ConversionException {

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
                throw new ContentDiffException("Can not make a content diff of documents without a blob");
            }

            adaptedDocBlob = adaptedDocBlobHolder.getBlob();
            otherDocBlob = otherDocBlobHolder.getBlob();
            if (adaptedDocBlob == null || otherDocBlob == null) {
                throw new ContentDiffException("Can not make a content diff of documents without a blob");
            }
        } catch (ClientException ce) {
            throw new ContentDiffException("Error while getting blobs", ce);
        }

        List<Blob> blobResults = new ArrayList<Blob>();

        String adaptedDocMimeType = getMimeType(adaptedDocBlob);
        String otherDocMimeType = getMimeType(otherDocBlob);
        log.debug("Mime type of adapted doc for HTML content diff = " + adaptedDocMimeType);
        log.debug("Mime type of other doc for HTML content diff = " + otherDocMimeType);

        // Check doc mime types, if a common mime type is found, look for the
        // associated content differ.
        if (adaptedDocMimeType != null && otherDocMimeType != null && adaptedDocMimeType.equals(otherDocMimeType)) {
            MimeTypeContentDiffer mtContentDiffer = getContentDiffAdapterManager().getContentDiffer(adaptedDocMimeType);
            if (mtContentDiffer != null) {
                // If using the HtmlContentDiffer for non HTML blobs
                // (text/plain, text/xml), we need to transform the blob strings
                // to encode XML entities and replace all occurrences of "\n"
                // with "<br />", since they will then be displayed in HTML.
                if (mtContentDiffer instanceof HtmlContentDiffer && !"text/html".equals(adaptedDocMimeType)) {
                    adaptedDocBlob = getHtmlStringBlob(adaptedDocBlob);
                    otherDocBlob = getHtmlStringBlob(otherDocBlob);
                }
                blobResults = mtContentDiffer.getContentDiff(adaptedDocBlob, otherDocBlob, locale);
                return blobResults;
            }
        }

        // Docs have a different mime type or no content differ found for the
        // common mime type.
        // Fall back on a conversion (conversionType) + HtmlContentDiffer.
        try {
            // Default conversion type is HTML
            if (conversionType == null) {
                conversionType = ContentDiffConversionType.html;
            }
            String converterName = conversionType.getValue();
            BlobHolder adaptedDocConvertedBlobHolder = getConvertedBlobHolder(adaptedDocBlobHolder, converterName);
            BlobHolder otherDocConvertedBlobHolder = getConvertedBlobHolder(otherDocBlobHolder, converterName);
            Blob adaptedDocConvertedBlob = adaptedDocConvertedBlobHolder.getBlob();
            Blob otherDocConvertedBlob = otherDocConvertedBlobHolder.getBlob();

            // In the case of a text conversion, we need to transform the blob
            // strings to encode XML entities and replace all occurrences of
            // "\n" with "<br />", since they will then be displayed in HTML by
            // the HtmlContentDiffer.
            if (ContentDiffConversionType.text.equals(conversionType)) {
                adaptedDocConvertedBlob = getHtmlStringBlob(adaptedDocConvertedBlob);
                otherDocConvertedBlob = getHtmlStringBlob(otherDocConvertedBlob);
            }

            // Add html content diff blob
            MimeTypeContentDiffer contentDiffer = getContentDiffAdapterManager().getHtmlContentDiffer();
            blobResults.addAll(contentDiffer.getContentDiff(adaptedDocConvertedBlob, otherDocConvertedBlob, locale));

            // Add secondary blobs (mostly images)
            addSecondaryBlobs(blobResults, adaptedDocConvertedBlobHolder, adaptedDocConvertedBlob.getFilename());
            addSecondaryBlobs(blobResults, otherDocConvertedBlobHolder, otherDocConvertedBlob.getFilename());
        } catch (ConversionException ce) {
            throw ce;
        } catch (ClientException ce) {
            throw new ContentDiffException("Error while getting HTML content diff on converted blobs", ce);
        }
        return blobResults;
    }

    public void cleanup() {
        // Nothing to do here
    }

    public boolean cachable() {
        return true;
    }

    public void setDefaultContentDiffFieldXPath(String xPath) {
        defaultFieldXPath = xPath;
    }

    protected BlobHolder getBlobHolder(DocumentModel doc, String xPath) throws ClientException {
        // TODO: manage other property types than Blob / String?
        Serializable prop = doc.getPropertyValue(xPath);
        if (prop instanceof Blob) {
            return new DocumentBlobHolder(doc, xPath);
        }
        if (prop instanceof String) {
            // Default mime type is text/plain. For a Note, use the
            // "note:mime_type" property, otherwise if the property value is
            // HTML use text/html.
            String mimeType = "text/plain";
            if ("note:note".equals(xPath)) {
                mimeType = (String) doc.getPropertyValue("note:mime_type");
            } else {
                if (HtmlGuesser.isHtml((String) prop)) {
                    mimeType = "text/html";
                }
            }
            return new DocumentStringBlobHolder(doc, xPath, mimeType);
        }
        throw new ClientException(String.format("Cannot get BlobHolder for doc '%s' and xpath '%s'.", doc.getTitle(),
                xPath));
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
                srcMT = mtr.getMimetypeFromFilenameAndBlobWithDefault(blob.getFilename(), blob,
                        "application/octet-stream");
                log.debug("mime type service returned " + srcMT);
            } catch (Exception e) {
                log.warn("error while calling Mimetype service", e);
            }
        }
        return srcMT;
    }

    protected void setMimeType(BlobHolder result) throws ClientException {
        for (Blob blob : result.getBlobs()) {
            if (blob.getMimeType() == null && blob.getFilename().endsWith("html")) {
                String mimeTpye = getMimeType(blob);
                blob.setMimeType(mimeTpye);
            }
        }
    }

    protected String getDefaultContentDiffFieldXPath() {
        return defaultFieldXPath;
    }

    /**
     * Returns a blob holder converted using the specified converter name.
     *
     * @param blobHolder the blob holder
     * @param converterName the converter name
     * @return the converted blob holder
     * @throws ClientException if an error occurs while getting the conversion service
     * @throws ConversionException if an error occurs while converting the blob holder
     */
    protected BlobHolder getConvertedBlobHolder(BlobHolder blobHolder, String converterName)
            throws ConversionException, ClientException {

        if (converterName == null) {
            log.debug(String.format("No converter parameter, using generic one: '%s'.", DEFAULT_CONVERTER_NAME));
            converterName = DEFAULT_CONVERTER_NAME;
        }

        BlobHolder convertedBlobHolder = getConversionService().convert(converterName, blobHolder, null);
        setMimeType(convertedBlobHolder);
        return convertedBlobHolder;
    }

    protected Blob getHtmlStringBlob(Blob blob) throws ContentDiffException {
        try {
            Blob htmlStringBlob = Blobs.createBlob(StringEscapeUtils.escapeHtml(
                    new String(blob.getByteArray(), "UTF-8")).replace("\r\n", "\n").replace("\n", "<br />"));
            htmlStringBlob.setFilename(blob.getFilename());
            return htmlStringBlob;
        } catch (IOException ioe) {
            throw new ContentDiffException(String.format("Could not get string from blob %s", blob.getFilename()), ioe);
        }
    }

    protected void addSecondaryBlobs(List<Blob> blobResults, BlobHolder blobHolder, String mainBlobFilename)
            throws ClientException {

        for (Blob blob : blobHolder.getBlobs()) {
            String blobFilename = blob.getFilename();
            if (blobFilename != null && !blobFilename.equals(mainBlobFilename)) {
                blobResults.add(blob);
            }
        }
    }

    /**
     * Gets the conversion service.
     *
     * @return the conversion service
     * @throws ClientException the client exception
     */
    protected final ConversionService getConversionService() throws ClientException {

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
