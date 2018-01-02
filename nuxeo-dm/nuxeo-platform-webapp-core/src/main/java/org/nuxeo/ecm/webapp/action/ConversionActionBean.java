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
 * Contributors:
 *     Nuxeo - initial API and implementation
 *     Florent Bonnet <florent.bonnet@nuxeo.com>
 *     Estelle Giuly <egiuly@nuxeo.com>
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.action;

import static org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry.PDF_MIMETYPE;
import static org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry.PDF_EXTENSION;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.jboss.seam.annotations.web.RequestParameter;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobManager.UsageHint;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.ecm.core.convert.api.ConverterNotRegistered;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.cache.ThreadSafeCacheHolder;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.runtime.api.Framework;

@Name("conversionActions")
@Scope(ScopeType.EVENT)
public class ConversionActionBean implements ConversionAction {

    private static final Log log = LogFactory.getLog(ConversionActionBean.class);

    protected Map<String, ConverterCheckResult> pdfConverterForTypes;

    @In(create = true, required = false)
    CoreSession documentManager;

    @In(create = true)
    NavigationContext navigationContext;

    @RequestParameter
    private String docRef;

    @RequestParameter
    private String fileFieldFullName;

    @RequestParameter
    private String filename;

    protected String pdfConverterName;

    protected static final ThreadSafeCacheHolder<Boolean> exportableToPDFCache = new ThreadSafeCacheHolder<>(20);

    public String display() {
        return "view_file";
    }

    private DocumentModel getDocument() {
        if (docRef == null) {
            return navigationContext.getCurrentDocument();
        } else {
            return documentManager.getDocument(new IdRef(docRef));
        }
    }

    private String getMimetypeFromDocument(String propertyName) {
        Blob blob = (Blob) getDocument().getPropertyValue(propertyName);
        return blob.getMimeType();
    }

    @Override
    public void reCheckConverterAvailability() {
        pdfConverterForTypes.clear();
    }

    public boolean isExportableToPDF(BlobHolder bh) {
        if (bh == null) {
            return false;
        }
        Blob blob = bh.getBlob();
        return blob != null && isExportableToPDF(blob);
    }

    @Override
    public boolean isExportableToPDF(Blob blob) {
        if (blob == null) {
            return false;
        }
        // check if there's a conversion available
        if (getPDFConversionURL(blob) != null) {
            return true;
        }
        String mimeType = blob.getMimeType();
        return isMimeTypeExportableToPDF(mimeType);
    }

    protected String getPDFConversionURL(Blob blob) {
        BlobManager blobManager = Framework.getService(BlobManager.class);
        try {
            URI uri = blobManager.getAvailableConversions(blob, UsageHint.DOWNLOAD).get(PDF_MIMETYPE);
            if (uri != null) {
                return uri.toString();
            }
        } catch (IOException e) {
            log.error("Failed to retrieve available conversions", e);
        }
        return null;
    }

    protected boolean isMimeTypeExportableToPDF(String mimeType) {
        // Don't bother searching for NO MIME type.
        if (mimeType == null) {
            return false;
        }

        // Initialize the converter check result map.
        if (pdfConverterForTypes == null) {
            pdfConverterForTypes = new HashMap<>();
        }

        // Check if there is any saved ConverterCheckResult for the desired
        // MIME type.
        if (pdfConverterForTypes.containsKey(mimeType)) {
            return pdfConverterForTypes.get(mimeType).isAvailable();
        }

        try {
            ConverterCheckResult pdfConverterAvailability;
            ConversionService conversionService = Framework.getService(ConversionService.class);
            Iterator<String> converterNames = conversionService.getConverterNames(mimeType, PDF_MIMETYPE).iterator();
            while (converterNames.hasNext()) {
                pdfConverterName = converterNames.next();
                pdfConverterAvailability = conversionService.isConverterAvailable(pdfConverterName, true);

                // Save the converter availability for all the mime-types the
                // converter
                // supports.
                for (String supMimeType : pdfConverterAvailability.getSupportedInputMimeTypes()) {
                    pdfConverterForTypes.put(supMimeType, pdfConverterAvailability);
                }

                if (pdfConverterAvailability.isAvailable()) {
                    return true;
                }
            }
        } catch (ConverterNotRegistered e) {
            log.error("Error while testing PDF converter availability", e);
        }
        return false;
    }

    @Override
    @WebRemote
    public boolean isFileExportableToPDF(String fieldName) {
        DocumentModel doc = getDocument();
        Boolean cacheResult = exportableToPDFCache.getFromCache(doc, fieldName);
        boolean isSupported;
        if (cacheResult == null) {
            String mimetype = getMimetypeFromDocument(fieldName);
            isSupported = isMimeTypeExportableToPDF(mimetype);
            exportableToPDFCache.addToCache(doc, fieldName, isSupported);
        } else {
            isSupported = cacheResult;
        }
        return isSupported;
    }

    public String generatePdfFileFromBlobHolder(DocumentModel doc, BlobHolder bh) throws IOException {
        // redirect to the conversion URL when available
        Blob blob = bh.getBlob();
        String url = getPDFConversionURL(blob);
        if (url != null) {
            try {
                FacesContext.getCurrentInstance().getExternalContext().redirect(url);
                return null;
            } catch (IOException e) {
                //
            }
        }
        if (pdfConverterName == null) {
            log.error("No PDF converter was found.");
            return "pdf_generation_error";
        }
        Blob result = Framework.getService(ConversionService.class)
                               .convertToMimeType(MimetypeRegistry.PDF_MIMETYPE, bh, Collections.emptyMap())
                               .getBlob();
        if (result == null) {
            log.error("Transform service didn't return any resulting documents which is not normal.");
            return "pdf_generation_error";
        }
        String xpath;
        if (bh instanceof DocumentBlobHolder) {
            xpath = ((DocumentBlobHolder) bh).getXpath();
        } else {
            xpath = null;
        }
        ComponentUtils.download(doc, xpath, result, result.getFilename(), "pdfConversion");
        return null;
    }

    @Override
    @WebRemote
    public String generatePdfFile() throws IOException {
        DocumentModel doc = getDocument();
        BlobHolder bh = new DocumentBlobHolder(doc, fileFieldFullName);
        return generatePdfFileFromBlobHolder(doc, bh);
    }

    /**
     * @since 7.3
     */
    public boolean isPDF(BlobHolder bh) {
        if (bh == null) {
            return false;
        }
        Blob blob = bh.getBlob();
        return blob != null && isPDF(blob);
    }

    /**
     * @since 7.3
     */
    public boolean isPDF(Blob blob) {
        if (blob == null) {
            return false;
        }
        String mimeType = blob.getMimeType();
        if (StringUtils.isNotBlank(mimeType) && PDF_MIMETYPE.equals(mimeType)) {
            return true;
        } else {
            String filename = blob.getFilename();
            if (StringUtils.isNotBlank(filename) && filename.endsWith(PDF_EXTENSION)) {
                // assume it's a pdf file
                return true;
            }
        }
        return false;
    }

    public void initialize() {
        // NOP
    }

}
