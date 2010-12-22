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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

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
import org.nuxeo.ecm.core.convert.api.ConversionException;
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

    private static final String ENABLE_CACHE_HEADER = "opensocial.features.enableCacheHeader";

    private static final String DTEFORMAT = "ddMMMyyyyHH:mm:ss z";

    private static final String GADGET_HTML_CONTENT = "gadget:htmlContent";

    private static final String GADGET_CONTENT_FILES = "gadgetContent";
    private static final String FILES_FILES = "files:files";
    private static final String FILENAME = "filename";
    private static final String FILE = "file";
    private static final String HTML_CONTENT = "htmlContent";

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
            /*
             * try to delete the file store in Files Schema with the
             * GADGET_CONTENT_FILES filename
             */
            doDeleteFiles(GADGET_CONTENT_FILES);
        }

        return Response.ok("File upload ok", MediaType.TEXT_PLAIN).build();
    }

    @POST
    @Path("delete/{filename}")
    public Response doDeleteFiles(@PathParam("filename") String filename) {
        try {
            removeFile(filename);
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
        return Response.ok("File upload ok", MediaType.TEXT_PLAIN).build();
    }

    @POST
    @Override
    public Response doPost() {
        String schema = null;
        FormData form = ctx.getForm();

        form.fillDocument(doc);

        if (form.isMultipartContent()) {

            Blob blob = form.getFirstBlob();
            if (blob == null) {
                throw new IllegalArgumentException(
                        "Could not find any uploaded file");
            } else {
                if (!"".equals(blob.getFilename())) {
                    try {

                        String crop = form.getString("crop");
                        if (crop != null) {
                            String[] dim = crop.split("x");
                            blob = getCroppedBlob(blob, Integer
                                    .parseInt(dim[0]), Integer.parseInt(dim[1]));
                        }
                        String resize = form.getString("resize_width");
                        if (resize != null) {
                            int resizeWidth = DEFAULT_SIZE_WIDTH;
                            try {
                                resizeWidth = Integer.parseInt(resize);
                            } catch (NumberFormatException e) {
                                log
                                        .info("No width for resize picture, use default size");
                            }
                            blob = getResizedBlobl(blob, resizeWidth);
                        }

                        schema = form.getString("schema");
                        if (schema != null) {
                            /* files are now stored in Files Schema */
                            String filename = form.getMultiPartFormProperty(FILENAME);
                            if (filename == null) {
                                filename = GADGET_CONTENT_FILES;
                            }
                            addFile(blob, filename);
                        } else {
                            String xpath = "file:content";
                            Property p = doc.getProperty(xpath);
                            p.getParent().get(FILENAME).setValue(blob.getFilename());
                            p.setValue(blob);
                        }

                    } catch (Exception e) {
                        throw WebException.wrap("Failed to attach file", e);
                    }
                }
            }
        }

        if(schema == null) {
            try {
                CoreSession session = getContext().getCoreSession();
                session.saveDocument(doc);
                session.save();
            } catch (ClientException e) {
                throw WebException.wrap(e);
            }
        }

        return Response.ok("File upload ok!", MediaType.TEXT_PLAIN).build();
    }

    protected Blob getCroppedBlob(Blob blob, int newWidth, int newHeight)
            throws IOException, ConversionException, ClientException {
        String fileName = blob.getFilename();
        blob.persist();

        Map<String, Serializable> options = new HashMap<String, Serializable>();
        ImageInfo imageInfo = getImagingService().getImageInfo(blob);
        int oldWidth = imageInfo.getWidth();
        int oldHeight = imageInfo.getHeight();

        /* if the picture's dimensions are smaller than the new dimensions */
        if (oldWidth < newWidth && oldHeight < newHeight){
            newWidth = oldWidth;
            newHeight = oldHeight;
        }
        /* if only the picture width is smaller than the new width */
        else if (oldWidth < newWidth && oldHeight > newHeight){
            double ratio = newWidth/newHeight;
            newHeight = (int) Math.round(oldWidth / ratio);
            newWidth = oldWidth;
        }
        /* if only the picture height is smaller than the new height */
        else if (oldHeight < newHeight && oldWidth > newWidth){
            double ratio = newWidth/newHeight;
            newWidth = (int) Math.round(oldHeight / ratio);
            newHeight = oldHeight;
        }
        blob = getImagingService().crop(blob, 0, 0, newWidth, newHeight);
        imageInfo = getImagingService().getImageInfo(blob);

        BlobHolder bh = new SimpleBlobHolder(blob);
        options.put(OPTION_RESIZE_WIDTH, newWidth);
        options.put(OPTION_RESIZE_HEIGHT, newHeight);
        options.put(OPTION_RESIZE_DEPTH, imageInfo.getDepth());
        options.put(CONVERSION_FORMAT, JPEG_CONVERSATION_FORMAT);
        bh = getConversionService().convert(OPERATION_RESIZE, bh, options);

        blob = bh.getBlob() != null ? bh.getBlob() : blob;
        String viewFilename = computeViewFilename(fileName,
                JPEG_CONVERSATION_FORMAT);
        blob.setFilename(viewFilename);

        return blob;
    }

    protected Blob getResizedBlobl(Blob blob, int newWidth)
            throws ClientException, IOException {
        String fileName = blob.getFilename();
        blob.persist();
        BlobHolder bh = new SimpleBlobHolder(blob);

        Map<String, Serializable> options = new HashMap<String, Serializable>();
        try {
            ImageInfo imageInfo = getImagingService()
                    .getImageInfo(bh.getBlob());

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
                conversionService = Framework
                        .getService(ConversionService.class);
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
            /* Check first if there are at least one file in files schema */
            ArrayList<Map<String, Serializable>> files = getFilesStoredInGadget();
            for (Map<String, Serializable> map : files) {
                if (map.get(FILE) != null && map.get(FILENAME) != null)
                    return Response.ok("true").build();
            }
            getBlobFromDoc(doc);
        } catch (Exception e) {
            throw new WebApplicationException(404);
        }

        return Response.ok("true").build();
    }

    @GET
    @Path("file")
    public Object getFile(@Context Request request) {
        Blob blob;
        try {
            /* try to get the file from file schema */
            blob = getBlobFromDoc(doc);
            return buildResponseToGetFile(request, blob);
        } catch (ClientException e1) {
            try {
                /*
                 * try to find the file from files schema with the default
                 * filename
                 */
                blob = getFileWithSpecificName(GADGET_CONTENT_FILES);
                return buildResponseToGetFile(request, blob);
            } catch (ClientException e) {
                throw WebException.wrap("Failed to get the attached file", e);
            }
        }

    }

    @GET
    @Path("file/{filename}")
    public Object getFile(@Context Request request,
            @PathParam("filename") String filename) {
        try {
            Blob blob = getFileWithSpecificName(filename);
            return buildResponseToGetFile(request, blob);
        } catch (ClientException e) {
            throw WebException.wrap("Failed to get the attached file", e);
        }
    }

    /**
     * Format the response to the getFile methods
     */
    private Response buildResponseToGetFile(Request request, Blob blob)
            throws PropertyException, ClientException {

        EntityTag tag = getEntityTagForDocument(doc);
        Calendar modified = (Calendar) doc.getPropertyValue("dc:modified");

        if (isCacheHeaderEnabled()) {
            Response.ResponseBuilder rb = request.evaluatePreconditions(
                    modified.getTime(), tag);
            if (rb != null) {
                return rb.build();
            }
        }

        String filename = blob.getFilename();
        String contentDisposition = "attachment;filename=" + filename;

        /*
         * Special handling for SWF file. Since Flash Player 10, Flash player
         * ignores reading if it sees Content-Disposition: attachment
         * http://forum.dokuwiki.org/thread/2894
         */
        if (filename.endsWith(".swf"))
            contentDisposition = "inline;";

        ResponseBuilder rb = Response.ok(blob).header("Content-Disposition",
                contentDisposition).type(blob.getMimeType());

        if (isCacheHeaderEnabled())
            rb = rb.lastModified(modified.getTime())
                    .expires(modified.getTime()).tag(tag);

        return rb.build();
    }

    private boolean isCacheHeaderEnabled() {
        String property = Framework.getProperty(ENABLE_CACHE_HEADER);
        return property != null && "true".equals(property);
    }

    static EntityTag getEntityTagForDocument(DocumentModel doc) {
        Calendar modified;
        try {
            modified = (Calendar) doc.getPropertyValue("dc:modified");
        } catch (ClientException e) {
            modified = Calendar.getInstance();
        }
        return new EntityTag(computeDigest(doc.getId()
                + new SimpleDateFormat(DTEFORMAT).format(modified.getTime())));
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
                    fileName = (String) p.getValue(FILENAME);
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
    public Object doGetHtml(@Context Request request) throws PropertyException,
            ClientException {
        EntityTag tag = getEntityTagForDocument(doc);
        Response.ResponseBuilder rb = request.evaluatePreconditions(tag);
        if (rb != null) {
            return rb.build();
        }

        /* If htmlContent is stored in files schema */
        Blob htmlContentBlob = getFileWithSpecificName(HTML_CONTENT);
        if (htmlContentBlob != null)
            return Response.ok(htmlContentBlob.toString(), MediaType.TEXT_HTML)
                    .build();

        String htmlContent = (String) doc.getPropertyValue(GADGET_HTML_CONTENT);
        return Response.ok(htmlContent, MediaType.TEXT_HTML).build();
    }

    protected String computeViewFilename(String filename, String format) {
        int index = filename.lastIndexOf(".");
        if (index == -1) {
            return filename + "." + format;
        } else {
            return filename.substring(0, index + 1) + format;
        }
    }

    /* Specific methods to get stored files in files schema */

    /**
     * Get the File with the specific name from the files schema
     */
    protected Blob getFileWithSpecificName(String filename)
            throws PropertyException, ClientException {
        ArrayList<Map<String, Serializable>> files = getFilesStoredInGadget();
        for (Map<String, Serializable> map : files) {
            if (map.containsValue(filename))
                if (map.get(FILE) != null) {
                    Blob htmlContentBlob = (Blob) map.get(FILE);
                    return htmlContentBlob;
                }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected ArrayList<Map<String, Serializable>> getFilesStoredInGadget()
            throws PropertyException, ClientException {
        return (ArrayList<Map<String, Serializable>>) doc
                .getPropertyValue(FILES_FILES);
    }

    /**
     * Update a file in files schema
     */
    public void addFile(Blob file, String filename) throws ClientException {
        try {
            ArrayList<Map<String, Serializable>> files = getFilesStoredInGadget();
            boolean isUpdate = false;
            for (Map<String, Serializable> map : files) {
                if (map.containsKey(FILENAME)
                        && filename.equals(map.get(FILENAME))) {
                    map.put(FILE, (Serializable) file);
                    isUpdate = true;
                    break;
                }
            }

            if (!isUpdate) {
                Map<String, Serializable> fileMap = new HashMap<String, Serializable>();
                fileMap.put(FILE, (Serializable) file);
                fileMap.put(FILENAME, filename);
                files.add(fileMap);
            }
            doc.setPropertyValue(FILES_FILES, files);
            CoreSession session = doc.getCoreSession();
            session.saveDocument(doc);
            session.save();
        } catch (PropertyException e) {
            log.error("No Property " + FILES_FILES + " for " + doc.getType());
        }
    }

    public void removeFile(String filename) throws ClientException {
        addFile(null, filename);
    }
}

