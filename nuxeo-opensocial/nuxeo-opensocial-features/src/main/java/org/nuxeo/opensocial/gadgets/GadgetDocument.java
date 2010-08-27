/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.opensocial.gadgets;

import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.CONVERSION_FORMAT;
import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.JPEG_CONVERSATION_FORMAT;
import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.OPERATION_RESIZE;
import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.OPTION_RESIZE_DEPTH;
import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.OPTION_RESIZE_HEIGHT;
import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.OPTION_RESIZE_WIDTH;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.runtime.api.Framework;

@WebObject(type = "GadgetDocument")
public class GadgetDocument extends DocumentObject {

    private static final String DTEFORMAT = "ddMMMyyyyHH:mm:ss z";

    private static final String GADGET_HTML_CONTENT = "gadget:htmlContent";

    private static final int DEFAULT_SIZE_WIDTH = 600;

    private ConversionService conversionService = null;

    private static final Log log = LogFactory.getLog(GadgetDocument.class);

    private ImagingService imagingService;


    @GET
    @Override
    public Object doGet() {
        return Response.serverError();
    }

    @POST
    @Path("deletePicture")
    public Response doDelete() {
        try {
            doc.setPropertyValue("file:content", null);

            CoreSession session = getContext().getCoreSession();
            session.saveDocument(doc);
            session.save();
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }

        return Response.ok("File upload ok", MediaType.TEXT_PLAIN)
                .build();
    }

    @POST
    @Override
    public Response doPost() {
        FormData form = ctx.getForm();

        form.fillDocument(doc);

        if (form.isMultipartContent()) {
            String xpath = "file:content";
            Blob blob = form.getFirstBlob();
            if (blob == null) {
                throw new IllegalArgumentException(
                        "Could not find any uploaded file");
            } else {
                if (!"".equals(blob.getFilename())) {
                    try {

                        String resize = form.getString("resize_width");
                        if (resize != null) {
                            int resizeWidth = DEFAULT_SIZE_WIDTH;
                            try {
                                resizeWidth = Integer.parseInt(resize);
                            } catch (NumberFormatException e) {
                                log.info("No width for resize picture, use default size");
                            }
                            blob = getResizedBlobl(blob, resizeWidth);
                        }

                        Property p = doc.getProperty(xpath);
                        p.getParent()
                                .get("filename")
                                .setValue(blob.getFilename());

                        p.setValue(blob);
                    } catch (Exception e) {
                        throw WebException.wrap("Failed to attach file", e);
                    }
                }
            }
        }

        try {
            CoreSession session = getContext().getCoreSession();
            session.saveDocument(doc);
            session.save();
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
        return Response.ok("File upload ok!", MediaType.TEXT_PLAIN)
                .build();
    }

    protected Blob getResizedBlobl(Blob blob, int newWidth)
            throws ClientException, IOException {
        String fileName = blob.getFilename();
        blob.persist();
        BlobHolder bh = new SimpleBlobHolder(blob);

        Map<String, Serializable> options = new HashMap<String, Serializable>();
        try {
            ImageInfo imageInfo = getImagingService().getImageInfo(bh.getBlob());

            int width, height, depth;
            width = imageInfo.getWidth();
            height = imageInfo.getHeight();
            depth = imageInfo.getDepth();

            double ratio = 1.0 * newWidth / width;

            int newHeight = (int) Math.round(height * ratio);

            options.put(OPTION_RESIZE_HEIGHT, newHeight);
            options.put(OPTION_RESIZE_DEPTH, depth);
        } catch (Exception e) {

        }

        options.put(OPTION_RESIZE_WIDTH, newWidth);
        // always convert to jpeg
        options.put(CONVERSION_FORMAT, JPEG_CONVERSATION_FORMAT);

        bh = getConversionService().convert(OPERATION_RESIZE, bh, options);

        blob = bh.getBlob() != null ? bh.getBlob() : blob;
        String viewFilename = computeViewFilename(fileName,
                JPEG_CONVERSATION_FORMAT);
        blob.setFilename(viewFilename);
        return blob;
    }

    protected ConversionService getConversionService() throws ClientException {
        if (conversionService == null) {
            try {
                conversionService = Framework.getService(ConversionService.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        return conversionService;
    }

    protected ImagingService getImagingService() {
        if (imagingService == null) {
            try {
                imagingService = Framework.getService(ImagingService.class);
            } catch (Exception e) {

            }

        }
        return imagingService;
    }

    @GET
    @Path("hasFile")
    public Response hasFile() {

        try {
            getBlobFromDoc(doc);
        } catch (Exception e) {
            return Response.ok("false")
                    .build();
        }

        return Response.ok("true")
                .build();

    }

    @GET
    @Path("file")
    public Object getFile(@Context Request request) {
        try {
            Calendar modified = (Calendar) doc.getPropertyValue("dc:modified");
            EntityTag tag = getEntityTagForDocument(doc);
            Response.ResponseBuilder rb = request.evaluatePreconditions(modified.getTime(), tag);
            if (rb != null) {
                throw new WebApplicationException(rb.build());
            }

            Blob blob = getBlobFromDoc(doc);
            String filename = blob.getFilename();



            String contentDisposition = "attachment;filename=" + filename;

            // Special handling for SWF file. Since Flash Player 10, Flash
            // player
            // ignores reading if it sees Content-Disposition: attachment
            // http://forum.dokuwiki.org/thread/2894
            if (filename.endsWith(".swf")) {
                contentDisposition = "inline;";
            }

            return Response.ok(blob)
                    .header("Content-Disposition", contentDisposition)
                    .type(blob.getMimeType())
                    .lastModified(modified.getTime())
                    .expires(modified.getTime())
                    .tag(tag)
                    .build();
        } catch (Exception e) {
            throw WebException.wrap("Failed to get the attached file", e);
        }
    }

    static EntityTag getEntityTagForDocument(DocumentModel doc) {
        Calendar modified;
        try {
            modified = (Calendar) doc.getPropertyValue("dc:modified");
        } catch (ClientException e) {
            modified = Calendar.getInstance();
        }
        return new EntityTag(computeDigest(doc.getId() + new SimpleDateFormat(DTEFORMAT).format(modified.getTime())));
    }

    private static String computeDigest(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            byte[] digest = md.digest(content.getBytes());
            BigInteger bi = new BigInteger(digest);
            return bi.toString(16);
        } catch (Exception e) {
            return "";
        }
    }

    private Blob getBlobFromDoc(DocumentModel doc) throws ClientException {
        String xpath = "file:content";

        Property p = doc.getProperty(xpath);
        Blob blob = (Blob) p.getValue();

        if (blob == null) {
            throw new WebResourceNotFoundException("No attached file at "
                    + xpath);
        }
        String fileName = blob.getFilename();
        if (fileName == null) {
            p = p.getParent();
            if (p.isComplex()) { // special handling for file and files
                // schema
                try {
                    fileName = (String) p.getValue("filename");
                } catch (PropertyException e) {
                    fileName = "Unknown";
                }
            }
            blob.setFilename(fileName);
        }

        return blob;
    }

    @GET
    @Path("html")
    public Object doGetHtml(@Context Request request) throws PropertyException, ClientException {
        EntityTag tag = getEntityTagForDocument(doc);
        Response.ResponseBuilder rb = request.evaluatePreconditions(tag);
        if (rb != null) {
            throw new WebApplicationException(rb.build());
        }
        String htmlContent = (String) doc.getPropertyValue(GADGET_HTML_CONTENT);
        return Response.ok(htmlContent, MediaType.TEXT_HTML)
                .build();
    }

    protected String computeViewFilename(String filename, String format) {
        int index = filename.lastIndexOf(".");
        if (index == -1) {
            return filename + "." + format;
        } else {
            return filename.substring(0, index + 1) + format;
        }
    }

}
