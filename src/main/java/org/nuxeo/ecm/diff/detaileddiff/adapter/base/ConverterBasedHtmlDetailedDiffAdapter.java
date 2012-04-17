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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.diff.detaileddiff.DetailedDiffException;
import org.nuxeo.ecm.diff.detaileddiff.adapter.DetailedDiffAdapterManager;
import org.nuxeo.ecm.diff.detaileddiff.adapter.MimeTypeDetailedDiffer;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.MimetypeNotFoundException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;

/**
 * Base class for detailed diff based on "on the fly" HTML transformers
 *
 * @author Antoine Taillefer
 * @since 5.6
 */
public class ConverterBasedHtmlDetailedDiffAdapter extends
        AbstractHtmlDetailedDiffAdapter {

    private static final Log log = LogFactory.getLog(ConverterBasedHtmlDetailedDiffAdapter.class);

    protected static DetailedDiffAdapterManager detailedDiffManager;

    protected static ConversionService cs;

    protected String defaultFieldXPath;

    protected MimetypeRegistry mimeTypeService;

    public static ConversionService getConversionService() throws Exception {
        if (cs == null) {
            cs = Framework.getService(ConversionService.class);
        }
        return cs;
    }

    @Override
    protected DetailedDiffAdapterManager getDetailedDiffManager()
            throws DetailedDiffException {
        if (detailedDiffManager == null) {
            try {
                detailedDiffManager = Framework.getService(DetailedDiffAdapterManager.class);
            } catch (Exception e) {
                throw new DetailedDiffException(e);
            }
        }
        return detailedDiffManager;
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
     * protected static boolean canHaveHtmlPreview(Blob blob) throws Exception {
     * if (blob == null) { return false; } String srcMT = getMimeType(blob);
     * return getTransformService().getPluginByMimeTypes(srcMT, "text/html") !=
     * null; }
     */

    protected String getDefaultDetailedDiffFieldXPath() {
        return defaultFieldXPath;
    }

    public void setDefaultDetailedDiffFieldXPath(String xPath) {
        defaultFieldXPath = xPath;
    }

    @Override
    public List<Blob> getDetailedDiffBlobs(DocumentModel otherDoc)
            throws DetailedDiffException {
        return getDetailedDiffBlobs(otherDoc,
                getDefaultDetailedDiffFieldXPath());
    }

    @Override
    public List<Blob> getDetailedDiffBlobs(DocumentModel otherDoc, String xpath)
            throws DetailedDiffException {

        Blob blob2DetailedDiff = null;
        Blob otherBlob2DetailedDiff = null;
        BlobHolder blobHolder2DetailedDiff = null;
        BlobHolder otherBlobHolder2DetailedDiff = null;

        if ((xpath == null) || ("default".equals(xpath))) {
            blobHolder2DetailedDiff = adaptedDoc.getAdapter(BlobHolder.class);
            otherBlobHolder2DetailedDiff = otherDoc.getAdapter(BlobHolder.class);
        } else {
            blobHolder2DetailedDiff = new DocumentBlobHolder(adaptedDoc, xpath);
            otherBlobHolder2DetailedDiff = new DocumentBlobHolder(otherDoc,
                    xpath);
        }

        try {
            blob2DetailedDiff = blobHolder2DetailedDiff.getBlob();
            otherBlob2DetailedDiff = otherBlobHolder2DetailedDiff.getBlob();
        } catch (ClientException e) {
            throw new DetailedDiffException("Error while getting blobs", e);
        }

        if (blob2DetailedDiff == null || otherBlob2DetailedDiff == null) {
            throw new DetailedDiffException(
                    "Can not make a detailed diff of documents without a blob");
        }
        List<Blob> blobResults = new ArrayList<Blob>();

        // TODO: manage case where both docs don't have the same mime-type!
        // For now lets take the first one.
        String srcMT = getMimeType(blob2DetailedDiff);
        log.debug("Source type for HTML detailed diff =" + srcMT);
        MimeTypeDetailedDiffer mtDetailedDiffer = getDetailedDiffManager().getDetailedDiffer(
                srcMT);
        if (mtDetailedDiffer != null) {
            blobResults = mtDetailedDiffer.getDetailedDiff(blob2DetailedDiff,
                    otherBlob2DetailedDiff, adaptedDoc, otherDoc);
            return blobResults;
        }

        // TODO: here should probably make an html conversion of the blobs then
        // use the
        // default HTMLDetailedDiffer (daisydiff)

        // String converterName = null;
        // try {
        // converterName = getConversionService().getConverterName(srcMT,
        // "text/html");
        // } catch (Exception e1) {
        // throw new DetailedDiffException("Unable to get converter", e1);
        // }
        //
        // if (converterName == null) {
        // log.debug("No dedicated converter found, using generic");
        // converterName = "any2html";
        // }
        //
        // BlobHolder result;
        // try {
        // result = getConversionService().convert(converterName,
        // blobHolder2DetailedDiff, null);
        // setMimeType(result);
        // return result.getBlobs();
        // } catch (ConverterNotAvailable e) {
        // throw new PreviewException(e.getMessage(), e);
        // } catch (ConversionException e) {
        // throw new PreviewException("Error during conversion", e);
        // } catch (Exception e) {
        // throw new PreviewException("Unexpected Error", e);
        // }
        return null;

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
