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
package org.nuxeo.ecm.platform.preview.adapter.base;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterNotAvailable;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.MimetypeNotFoundException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.preview.adapter.MimeTypePreviewer;
import org.nuxeo.ecm.platform.preview.adapter.PreviewAdapterManager;
import org.nuxeo.ecm.platform.preview.api.PreviewException;
import org.nuxeo.runtime.api.Framework;

/**
 * Base class for preview based on "on the fly" HTML transformers
 *
 * @author tiry
 */
public class ConverterBasedHtmlPreviewAdapter extends
        AbstractHtmlPreviewAdapter {

    private static final Log log = LogFactory.getLog(ConverterBasedHtmlPreviewAdapter.class);

    protected static PreviewAdapterManager previewManager;

    protected static ConversionService cs;

    protected String defaultFieldXPath;

    protected MimetypeRegistry mimeTypeService;

    public static ConversionService getConversionService() throws Exception {
        if (cs==null) {
            cs = Framework.getService(ConversionService.class);
        }
        return cs;
    }

    @Override
    protected PreviewAdapterManager getPreviewManager() throws PreviewException {
        if (previewManager == null) {
            try {
                previewManager = Framework.getService(PreviewAdapterManager.class);
            } catch (Exception e) {
                throw new PreviewException(e);
            }
        }
        return previewManager;
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
                srcMT = mtr.getMimetypeFromFilenameAndBlobWithDefault(
                        blob.getFilename(), blob, "application/octet-stream");
                log.debug("mime type service returned " + srcMT);
            } catch (Exception e) {
                log.warn("error while calling Mimetype service", e);
            }
        }
        return srcMT;
    }

    /*
    protected static boolean canHaveHtmlPreview(Blob blob) throws Exception {
        if (blob == null) {
            return false;
        }
        String srcMT = getMimeType(blob);
        return getTransformService().getPluginByMimeTypes(srcMT, "text/html") != null;
    }*/

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

        Blob blob2Preview=null;
        BlobHolder blobHolder2preview=null;

        if ((xpath==null) || ("default".equals(xpath))) {
            blobHolder2preview = adaptedDoc.getAdapter(BlobHolder.class);
        }
        else {
            blobHolder2preview = new DocumentBlobHolder(adaptedDoc, xpath);
        }

        try {
            blob2Preview = blobHolder2preview.getBlob();
        } catch (ClientException e) {
            throw new PreviewException("Error while getting blob", e);
        }

        if (blob2Preview == null) {
            throw new PreviewException(
                    "can not preview a document without blob");
        }
        List<Blob> blobResults = new ArrayList<Blob>();

        String srcMT = getMimeType(blob2Preview);
        log.debug("Source type for HTML preview =" + srcMT);
        MimeTypePreviewer mtPreviewer = getPreviewManager().getPreviewer(srcMT);
        if (mtPreviewer != null) {
            blobResults = mtPreviewer.getPreview(blob2Preview, adaptedDoc);
            return blobResults;
        }

        String converterName=null;
        try {
            converterName = getConversionService().getConverterName(srcMT, "text/html");
        } catch (Exception e1) {
            throw new PreviewException("Unable to get converter", e1);
        }

        if (converterName == null) {
            log.debug("No dedicated converter found, using generic");
            converterName = "any2html";
        }


        BlobHolder result;
        try {
            result = getConversionService().convert(converterName, blobHolder2preview, null);
            setMimeType(result);
            return result.getBlobs();
        } catch (ConverterNotAvailable e) {
            throw new PreviewException(e.getMessage(), e);
        } catch (ConversionException e) {
            throw new PreviewException("Error during conversion", e);
        } catch (Exception e) {
            throw new PreviewException("Unexpected Error", e);
        }

    }

    protected void setMimeType(BlobHolder result) throws ClientException {
        for (Blob blob : result.getBlobs()) {
            if (blob.getMimeType() == null && blob.getFilename().endsWith("html")) {
                String mimeTpye = getMimeType(blob);
                blob.setMimeType(mimeTpye);
            }
        }
    }


    public String getMimeType(File file) throws ConversionException{
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
            try {
                mimeTypeService = Framework.getService(MimetypeRegistry.class);
            } catch (Exception e) {
                throw new ConversionException("Could not get MimeTypeRegistry");
            }
        }
        return mimeTypeService;
    }

    public void cleanup() {

    }

    public boolean cachable() {
        return true;
    }

}
