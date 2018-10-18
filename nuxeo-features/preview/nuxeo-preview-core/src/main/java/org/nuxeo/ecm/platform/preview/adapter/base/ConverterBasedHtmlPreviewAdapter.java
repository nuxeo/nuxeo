/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.preview.adapter.base;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.MimetypeNotFoundException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.preview.adapter.ImagePreviewer;
import org.nuxeo.ecm.platform.preview.adapter.MarkdownPreviewer;
import org.nuxeo.ecm.platform.preview.adapter.MimeTypePreviewer;
import org.nuxeo.ecm.platform.preview.adapter.OfficePreviewer;
import org.nuxeo.ecm.platform.preview.adapter.PdfPreviewer;
import org.nuxeo.ecm.platform.preview.adapter.PlainImagePreviewer;
import org.nuxeo.ecm.platform.preview.adapter.PreviewAdapterManager;
import org.nuxeo.ecm.platform.preview.api.NothingToPreviewException;
import org.nuxeo.ecm.platform.preview.api.PreviewException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Base class for preview based on "on the fly" HTML transformers
 *
 * @author tiry
 */
public class ConverterBasedHtmlPreviewAdapter extends AbstractHtmlPreviewAdapter {

    private static final Logger log = LogManager.getLogger(ConverterBasedHtmlPreviewAdapter.class);

    /**
     * @since 10.3
     * @deprecated since 10.3
     */
    public static final String OLD_PREVIEW_PROPERTY = "nuxeo.old.jsf.preview";

    /**
     * @since 10.3
     * @deprecated since 10.3
     */
    public static final String TEXT_ANNOTATIONS_PROPERTY = "nuxeo.text.annotations";

    protected String defaultFieldXPath;

    protected MimetypeRegistry mimeTypeService;

    /**
     * @since 8.10
     */
    protected static final String ALLOW_ZIP_PREVIEW = "nuxeo.preview.zip.enabled";

    public ConversionService getConversionService() {
        return Framework.getService(ConversionService.class);
    }

    @Override
    protected PreviewAdapterManager getPreviewManager() {
        return Framework.getService(PreviewAdapterManager.class);
    }

    protected static String getMimeType(Blob blob) {
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

    protected String getMimeType(String xpath) {
        BlobHolder blobHolder2preview = getBlobHolder2preview(xpath);
        Blob blob = getBlob2preview(blobHolder2preview);
        return getMimeType(blob);
    }

    protected String getDefaultPreviewFieldXPath() {
        return defaultFieldXPath;
    }

    public void setDefaultPreviewFieldXPath(String xPath) {
        defaultFieldXPath = xPath;
    }

    @Override
    public List<Blob> getPreviewBlobs() throws PreviewException {
        return getPreviewBlobs(getDefaultPreviewFieldXPath());
    }

    @Override
    public boolean hasPreview(String xpath) {
        String srcMT;
        try {
            srcMT = getMimeType(xpath);
        } catch (NothingToPreviewException e) {
            return false;
        }
        if ("application/zip".equals(srcMT)
                && !Framework.getService(ConfigurationService.class).isBooleanPropertyTrue(ALLOW_ZIP_PREVIEW)) {
            return false;
        }
        MimeTypePreviewer mtPreviewer = getPreviewManager().getPreviewer(srcMT);
        return mtPreviewer != null || getConversionService().getConverterName(srcMT, "text/html") != null;
    }

    @Override
    public List<Blob> getPreviewBlobs(String xpath) throws PreviewException {
        BlobHolder blobHolder2preview = getBlobHolder2preview(xpath);
        Blob blob2Preview;
        try {
            blob2Preview = getBlob2preview(blobHolder2preview);
        } catch (NothingToPreviewException e) {
            return Collections.emptyList();
        }

        String srcMT = getMimeType(xpath);
        log.debug("Source type for HTML preview =" + srcMT);
        MimeTypePreviewer mtPreviewer = getPreviewManager().getPreviewer(srcMT);
        if (mtPreviewer != null) {
            List<Blob> result = getPreviewFromMimeTypePreviewer(mtPreviewer, blob2Preview);
            if (result != null) {
                return result;
            }
        }

        String converterName = getConversionService().getConverterName(srcMT, "text/html");
        if (converterName == null) {
            log.debug("No dedicated converter found, using generic");
            converterName = "any2html";
        }

        BlobHolder result;
        try {
            result = getConversionService().convert(converterName, blobHolder2preview, null);
            setMimeType(result);
            setDigest(result);
            return result.getBlobs();
        } catch (ConversionException e) {
            throw new PreviewException(e.getMessage(), e);
        }
    }

    /**
     * Backward compatibility method to trigger the right previewers if 'nuxeo.old.jsf.preview' is set.
     * <p>
     * This allows old HTML preview to be used, to make annotations available.
     * <p>
     * To be removed with JSF UI.
     *
     * @since 10.3
     * @deprecated since 10.3
     */
    protected List<Blob> getPreviewFromMimeTypePreviewer(MimeTypePreviewer mtPreviewer, Blob blob2Preview) {
        // this context data comes from the PreviewRestlet
        boolean oldPreview = Boolean.TRUE.equals(adaptedDoc.getContextData(OLD_PREVIEW_PROPERTY));
        if (!oldPreview) {
            return mtPreviewer.getPreview(blob2Preview, adaptedDoc);
        }

        // when old preview is enabled
        // - replace ImagePreviewer with PlainImagePreviewer
        // - do nothing for "office" previewers if the text annotations are enabled to trigger the old preview behavior,
        // otherwise keep the current preview behavior
        if (mtPreviewer instanceof ImagePreviewer) {
            return new PlainImagePreviewer().getPreview(blob2Preview, adaptedDoc);
        }

        ConfigurationService cs = Framework.getService(ConfigurationService.class);
        if (cs.isBooleanPropertyTrue(TEXT_ANNOTATIONS_PROPERTY) && (mtPreviewer instanceof PdfPreviewer
                || mtPreviewer instanceof MarkdownPreviewer || mtPreviewer instanceof OfficePreviewer)) {
            return null;
        }
        return mtPreviewer.getPreview(blob2Preview, adaptedDoc);
    }

    /**
     * @since 5.7.3
     */
    private Blob getBlob2preview(BlobHolder blobHolder2preview) throws PreviewException {
        Blob blob2Preview;
        try {
            blob2Preview = blobHolder2preview.getBlob();
        } catch (PropertyNotFoundException e) {
            blob2Preview = null;
        }
        if (blob2Preview == null) {
            throw new NothingToPreviewException("Can not preview a document without blob");
        } else {
            return blob2Preview;
        }
    }

    /**
     * Returns a blob holder suitable for a preview.
     *
     * @since 5.7.3
     */
    private BlobHolder getBlobHolder2preview(String xpath) {
        if ((xpath == null) || ("default".equals(xpath))) {
            return adaptedDoc.getAdapter(BlobHolder.class);
        } else {
            return new DocumentBlobHolder(adaptedDoc, xpath);
        }
    }

    protected void setMimeType(BlobHolder result) {
        for (Blob blob : result.getBlobs()) {
            if ((blob.getMimeType() == null || blob.getMimeType().startsWith("application/octet-stream"))
                    && blob.getFilename().endsWith("html")) {
                String mimeTpye = getMimeType(blob);
                blob.setMimeType(mimeTpye);
            }
        }
    }

    protected void setDigest(BlobHolder result) {
        for (Blob blob : result.getBlobs()) {
            if (blob.getDigest() == null) {
                try (InputStream stream = blob.getStream()) {
                    String digest = DigestUtils.md5Hex(stream);
                    blob.setDigest(digest);
                } catch (IOException e) {
                    log.warn("Unable to compute digest of blob.", e);
                }
            }
        }
    }

    public String getMimeType(File file) throws ConversionException {
        try {
            return getMimeTypeService().getMimetypeFromFile(file);
        } catch (ConversionException e) {
            throw new ConversionException("Could not get MimeTypeRegistry");
        } catch (MimetypeNotFoundException | MimetypeDetectionException e) {
            return "application/octet-stream";
        }
    }

    public MimetypeRegistry getMimeTypeService() throws ConversionException {
        if (mimeTypeService == null) {
            mimeTypeService = Framework.getService(MimetypeRegistry.class);
        }
        return mimeTypeService;
    }

    @Override
    public void cleanup() {

    }

    @Override
    public boolean cachable() {
        return true;
    }

    @Override
    public boolean hasBlobToPreview() throws PreviewException {
        String xpath = getDefaultPreviewFieldXPath();
        Blob blob2Preview;
        try {
            blob2Preview = getBlob2preview(getBlobHolder2preview(xpath));
        } catch (NothingToPreviewException e) {
            return false;
        }
        String srcMT = getMimeType(xpath);
        if ("application/zip".equals(srcMT)
                && !Framework.getService(ConfigurationService.class).isBooleanPropertyTrue(ALLOW_ZIP_PREVIEW)) {
            return false;
        }
        return blob2Preview != null;
    }

}
