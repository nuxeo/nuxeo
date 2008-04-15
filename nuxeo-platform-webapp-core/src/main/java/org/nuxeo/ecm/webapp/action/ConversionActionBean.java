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
import java.util.List;

import javax.ejb.Remove;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.RequestParameter;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.WebRemote;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.transform.api.TransformServiceDelegate;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.interfaces.TransformServiceCommon;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:florent.bonnet@nuxeo.com">Florent BONNET</a>
 */
@Name("conversionActions")
@Transactional
public class ConversionActionBean implements ConversionAction {

    private static final Log log = LogFactory.getLog(ConversionActionBean.class);

    @In(create = true)
    NavigationContext navigationContext;

    @RequestParameter
    private String fileFieldFullName;

    @RequestParameter
    private String filename;

    @Remove
    public void destroy() {
        log.debug("Removing Seam action listener...");
    }

    public String display() {
        return "view_file";
    }

    private String getMimetypeFromDocument(String propertyName)
            throws PropertyException {
        Blob blob = (Blob) navigationContext.getCurrentDocument().getPropertyValue(
                propertyName);
        return blob.getMimeType();
    }

    public boolean isExportableToPDF(Blob blob) {
        boolean isSupported = false;

        try {
            if (blob != null) {
                String mimetype = blob.getMimeType();
                TransformServiceCommon nxt = TransformServiceDelegate.getRemoteTransformService();
                isSupported = nxt.isMimetypeSupportedByPlugin("any2pdf",
                        mimetype);
            }
        } catch (Exception e) {
            log.error("error asking the any2pdf plugin whether pdf conversion "
                    + " is supported: " + e.getMessage());
        }

        return isSupported;
    }

    @WebRemote
    public boolean isFileExportableToPDF(String fieldName) {
        boolean isSupported = false;

        try {
            String mimetype = getMimetypeFromDocument(fieldName);
            TransformServiceCommon nxt = TransformServiceDelegate.getRemoteTransformService();
            isSupported = nxt.isMimetypeSupportedByPlugin("any2pdf", mimetype);
        } catch (Exception e) {
            log.error("error asking the any2pdf plugin whether " + fieldName
                    + " is supported: " + e.getMessage());
        }

        return isSupported;
    }

    @WebRemote
    public String generatePdfFile() {
        try {

            if (fileFieldFullName == null) {
                return null;
            }

            Blob blob = (Blob) navigationContext.getCurrentDocument().getPropertyValue(
                    fileFieldFullName);

            TransformServiceCommon nxt = Framework.getService(TransformServiceCommon.class);
            List<TransformDocument> resultingDocs = nxt.transform("any2pdf",
                    null, blob);

            String name;
            if (filename == null || filename.equals("")) {
                name = "file";
            } else {
                name = filename;
            }

            // add pdf extension
            int pos = name.lastIndexOf(".");
            if (pos <= 0) {
                name += ".pdf";
            } else {
                String sub = name.substring(pos + 1);
                name = name.replace(sub, "pdf");
            }

            if (resultingDocs.size() == 0) {
                log.error("Transform service didn't return any resulting documents which is not normal.");
                return "pdf_generation_error";
            }

            // converting the result into byte[] to be able to put it in the
            // response
            InputStream inputStream = resultingDocs.get(0).getBlob().getStream();
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

    /**
     * @deprecated use LiveEditBootstrapHelper.isCurrentDocumentLiveEditable()
     *             instead
     */
    @Deprecated
    @WebRemote
    public boolean isFileOnlineEditable(String fieldName) {
        try {
            boolean isOnlineEditable;
            String mimetype = getMimetypeFromDocument(fieldName);
            MimetypeRegistry mimeTypeService = Framework.getService(MimetypeRegistry.class);
            MimetypeEntry mimetypeEntry = mimeTypeService.getMimetypeEntryByMimeType(mimetype);
            if (mimetypeEntry == null) {
                isOnlineEditable = false;
            } else {
                isOnlineEditable = mimetypeEntry.isOnlineEditable();
            }
            return isOnlineEditable;
        } catch (Exception e) {
            log.error("error getting the mimetype entry for " + fieldName
                    + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Simply sends what to be downloaded or shown at screen via
     * HttpServletResponse.
     *
     * @param header
     * @param headerContent
     * @param contentType
     * @param value
     * @throws IOException
     */
    private void writeResponse(String header, String headerContent,
            String contentType, byte[] value) throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        response.setHeader(header, headerContent);
        response.setContentType(contentType);
        response.getOutputStream().write(value);
        context.responseComplete();
    }

    public void initialize() {
        log.info("initializing FileViewAction");
    }

}
