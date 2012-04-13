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
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.action;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.jboss.seam.annotations.web.RequestParameter;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.cache.ThreadSafeCacheHolder;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:florent.bonnet@nuxeo.com">Florent BONNET</a>
 */
@Name("conversionActions")
@Scope(ScopeType.EVENT)
public class ConversionActionBean implements ConversionAction {

    private static final Log log = LogFactory.getLog(ConversionActionBean.class);

    protected Map<String, ConverterCheckResult> pdfConverterForTypes;

    protected static final String PDF_MIMETYPE = "application/pdf";

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

    protected static final ThreadSafeCacheHolder<Boolean> exportableToPDFCache = new ThreadSafeCacheHolder<Boolean>(
            20);

    public String display() {
        return "view_file";
    }

    private DocumentModel getDocument() throws ClientException {
        if (docRef == null) {
            return navigationContext.getCurrentDocument();
        } else {
            return documentManager.getDocument(new IdRef(docRef));
        }
    }

    private String getMimetypeFromDocument(String propertyName)
            throws ClientException {
        Blob blob = (Blob) getDocument().getPropertyValue(propertyName);
        return blob.getMimeType();
    }

    public void reCheckConverterAvailability() {
        pdfConverterForTypes.clear();
    }

    public boolean isExportableToPDF(BlobHolder bh) throws ClientException {
        if (bh == null) {
            return false;
        }
        Blob blob = bh.getBlob();
        if (blob == null) {
            return false;
        } else {
            return isExportableToPDF(blob);
        }
    }

    public boolean isExportableToPDF(Blob blob) {
        if (blob == null) {
            return false;
        }
        String mimetype = blob.getMimeType();
        return isMimeTypeExportableToPDF(mimetype);
    }

    protected boolean isMimeTypeExportableToPDF(String mimetype) {
        // Don't bother searching for NO MIME type.
        if (mimetype == null) {
            return false;
        }

        // Initialize the converter check result map.
        if (pdfConverterForTypes == null) {
            pdfConverterForTypes = new HashMap<String, ConverterCheckResult>();
        }

        // Check if there is any saved ConverterCheckResult for the desired MIME type.
        if (pdfConverterForTypes.containsValue(mimetype)) {
            return pdfConverterForTypes.get(mimetype).isAvailable();
        }

        try {
            ConverterCheckResult pdfConverterAvailability;
            ConversionService conversionService = Framework.getLocalService(ConversionService.class);
            Iterator<String> converterNames = conversionService.getConverterNames(
                    mimetype, PDF_MIMETYPE).iterator();
            while (converterNames.hasNext()) {
                pdfConverterName = converterNames.next();
                pdfConverterAvailability = conversionService.isConverterAvailable(
                        pdfConverterName, true);

                // Save the converter availability for all the mime-types the converter
                // supports.
                for (String supMimeType : pdfConverterAvailability.getSupportedInputMimeTypes()) {
                    pdfConverterForTypes.put(supMimeType, pdfConverterAvailability);
                }

                if (pdfConverterAvailability.isAvailable()) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("Error while testing PDF converter availability", e);
        }
        return false;
    }

    @WebRemote
    public boolean isFileExportableToPDF(String fieldName) {
        try {
            DocumentModel doc = getDocument();
            Boolean cacheResult = exportableToPDFCache.getFromCache(doc,
                    fieldName);
            boolean isSupported;
            if (cacheResult == null) {
                String mimetype = getMimetypeFromDocument(fieldName);
                isSupported = isMimeTypeExportableToPDF(mimetype);
                exportableToPDFCache.addToCache(doc, fieldName, isSupported);
            } else {
                isSupported = cacheResult;
            }
            return isSupported;
        } catch (Exception e) {
            log.error(
                    "Error while trying to check PDF conversion against a filename",
                    e);
            return false;
        }
    }

    public String generatePdfFileFromBlobHolder(BlobHolder bh) {
        try {
            if (pdfConverterName == null) {
                log.error("No PDF converter was found.");
                return "pdf_generation_error";
            }

            BlobHolder result = Framework.getLocalService(
                    ConversionService.class).convert(pdfConverterName, bh, null);

            String fname = new Path(bh.getFilePath()).lastSegment();
            String name;
            if (fname == null || fname.length() == 0) {
                name = "file";
            } else {
                name = fname;
            }
            // add pdf extension
            int pos = name.lastIndexOf('.');
            if (pos > 0) {
                name = name.substring(0, pos);
            }
            name += ".pdf";

            if (result == null) {
                log.error("Transform service didn't return any resulting documents which is not normal.");
                return "pdf_generation_error";
            }

            // converting the result into byte[] to be able to put it in the
            // response
            InputStream inputStream = result.getBlob().getStream();
            int length = inputStream.available();
            byte[] array = new byte[length];
            int offset = 0;
            int n;
            do {
                n = inputStream.read(array, offset, length - offset);
            } while (n != -1);

            String headerContent = "attachment; filename=\"" + name + "\";";
            writeResponse("Content-Disposition", headerContent,
                    "application/pdf", array);

            return null;
        } catch (Exception e) {
            log.error("PDF generation error for file " + filename, e);
        }
        return "pdf_generation_error";

    }

    @WebRemote
    public String generatePdfFile() {
        try {
            BlobHolder bh = new DocumentBlobHolder(getDocument(),
                    fileFieldFullName);
            return generatePdfFileFromBlobHolder(bh);
        } catch (Exception e) {
            log.error("PDF generation error for file " + filename, e);
        }
        return "pdf_generation_error";
    }

    /**
     * Simply sends what to be downloaded or shown at screen via
     * HttpServletResponse.
     */
    private static void writeResponse(String header, String headerContent,
            String contentType, byte[] value) throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        response.setHeader(header, headerContent);
        response.setContentType(contentType);
        response.getOutputStream().write(value);
        context.responseComplete();
    }

    public void initialize() {
        // NOP
    }

}
