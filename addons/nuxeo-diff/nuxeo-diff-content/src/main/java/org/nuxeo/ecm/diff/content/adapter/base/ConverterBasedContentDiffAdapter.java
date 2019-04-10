/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.diff.content.adapter.base;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.StringEscapeUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.diff.content.ContentDiffException;
import org.nuxeo.ecm.diff.content.ContentDiffHelper;
import org.nuxeo.ecm.diff.content.adapter.HtmlContentDiffer;
import org.nuxeo.ecm.diff.content.adapter.MimeTypeContentDiffer;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
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

        Blob adaptedDocBlob;
        Blob otherDocBlob;
        BlobHolder adaptedDocBlobHolder;
        BlobHolder otherDocBlobHolder;

        if ((xpath == null) || (ContentDiffHelper.DEFAULT_XPATH.equals(xpath))) {
            adaptedDocBlobHolder = adaptedDoc.getAdapter(BlobHolder.class);
            otherDocBlobHolder = otherDoc.getAdapter(BlobHolder.class);
        } else {
            adaptedDocBlobHolder = ContentDiffHelper.getBlobHolder(adaptedDoc, xpath);
            otherDocBlobHolder = ContentDiffHelper.getBlobHolder(otherDoc, xpath);
        }
        if (adaptedDocBlobHolder == null || otherDocBlobHolder == null) {
            throw new ContentDiffException("Can not make a content diff of documents without a blob");
        }

        adaptedDocBlob = adaptedDocBlobHolder.getBlob();
        otherDocBlob = otherDocBlobHolder.getBlob();
        if (adaptedDocBlob == null || otherDocBlob == null) {
            throw new ContentDiffException("Can not make a content diff of documents without a blob");
        }

        List<Blob> blobResults = new ArrayList<>();

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
        return blobResults;
    }

    @Override
    public void cleanup() {
        // Nothing to do here
    }

    @Override
    public boolean cachable() {
        return true;
    }

    public void setDefaultContentDiffFieldXPath(String xPath) {
        defaultFieldXPath = xPath;
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
            } catch (MimetypeDetectionException e) {
                log.warn("error while calling Mimetype service", e);
            }
        }
        return srcMT;
    }

    protected void setMimeType(BlobHolder result) {
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
     * @throws ConversionException if an error occurs while converting the blob holder
     */
    protected BlobHolder getConvertedBlobHolder(BlobHolder blobHolder, String converterName)
            throws ConversionException {

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
            String htmlString = StringEscapeUtils.escapeHtml4(new String(blob.getByteArray(), "UTF-8"))
                                                 .replace("\r\n", "\n")
                                                 .replace("\n", "<br />");
            Blob htmlStringBlob = Blobs.createBlob(htmlString);
            htmlStringBlob.setFilename(blob.getFilename());
            return htmlStringBlob;
        } catch (IOException ioe) {
            throw new ContentDiffException(String.format("Could not get string from blob %s", blob.getFilename()), ioe);
        }
    }

    protected void addSecondaryBlobs(List<Blob> blobResults, BlobHolder blobHolder, String mainBlobFilename) {

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
     */
    protected final ConversionService getConversionService() {
        ConversionService conversionService = Framework.getService(ConversionService.class);
        if (conversionService == null) {
            throw new NuxeoException("ConversionService service is null.");
        }
        return conversionService;
    }

}
