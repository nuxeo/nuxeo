/*
 * (C) Copyright 2006-20014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package org.nuxeo.ecm.platform.preview.adapter.base;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.MimetypeNotFoundException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.preview.adapter.MimeTypePreviewer;
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

    private static final Log log = LogFactory.getLog(ConverterBasedHtmlPreviewAdapter.class);

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
    public List<Blob> getPreviewBlobs(String xpath) throws PreviewException {

        BlobHolder blobHolder2preview = getBlobHolder2preview(xpath);
        Blob blob2Preview;
        try {
            blob2Preview = getBlob2preview(blobHolder2preview);
        } catch (NothingToPreviewException e) {
            return Collections.emptyList();
        }

        List<Blob> blobResults = new ArrayList<>();

        String srcMT = getMimeType(blob2Preview);
        log.debug("Source type for HTML preview =" + srcMT);
        MimeTypePreviewer mtPreviewer = getPreviewManager().getPreviewer(srcMT);
        if (mtPreviewer != null) {
            blobResults = mtPreviewer.getPreview(blob2Preview, adaptedDoc);
            return blobResults;
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
     * @param blobHolder2preview
     * @return
     * @throws PreviewException
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
     * @param xpath
     * @param adaptedDoc
     * @return
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
        } catch (MimetypeNotFoundException e) {
            return "application/octet-stream";
        } catch (MimetypeDetectionException e) {
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
        if (blob2Preview == null || ("application/zip".equals(getMimeType(blob2Preview))
                && !Framework.getService(ConfigurationService.class).isBooleanPropertyTrue(ALLOW_ZIP_PREVIEW))) {
            return false;
        }
        return true;
    }

}
