package org.nuxeo.ecm.platform.convert.plugins;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.net.ConnectException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.streaming.FileSource;

import com.artofsolving.jodconverter.DocumentFamily;
import com.artofsolving.jodconverter.DocumentFormat;
import com.artofsolving.jodconverter.DocumentFormatRegistry;
import com.artofsolving.jodconverter.XmlDocumentFormatRegistry;
import com.artofsolving.jodconverter.openoffice.connection.OpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.converter.OpenOfficeDocumentConverter;

public class JODBasedConverter implements Converter {


    private static final Log log = LogFactory.getLog(JODBasedConverter.class);

    private static final long serialVersionUID = 1L;

    private static final DocumentFormatRegistry formatRegistry = new XmlDocumentFormatRegistry();

    private static final String DEFAULT_OOO_HOST_URL = "localhost";

    private static final int DEFAULT_OOO_HOST_PORT = 8100;

    // OOo doesn't support multi thread connection on the same port.
    private static OpenOfficeConnection connection;

    private static final Lock conLock = new ReentrantLock();

    protected ConverterDescriptor descriptor;

    protected String getDestinationMimeType() {
        return descriptor.getDestinationMimeType();

    }
    /**
     * Returns the DestinationFormat for the given plugin.
     * <p>
     * It takes the actual destination mimetype of from the plugin
     * configuration.
     *
     * @see org.nuxeo.ecm.platform.transform.interfaces.Plugin
     *
     * @return the DestinationFormat for this given plugin.
     */
    private DocumentFormat getDestinationFormat() {
        return formatRegistry.getFormatByMimeType(getDestinationMimeType());
    }

    /**
     * Returns the DocumentFormat for the file given as a parameter.
     * <p>
     * We will ask the mimetype registry service to sniff its mimetype.
     *
     * @param file
     * @return DocumentFormat for the given file
     * @throws Exception
     */
    private static DocumentFormat getSourceFormat(File file) throws Exception {
        DocumentFormat format;
        MimetypeRegistry mimetypeRegistry = Framework.getService(MimetypeRegistry.class);
        String mimetypeStr = mimetypeRegistry.getMimetypeFromFile(file);
        // TODO: JODconverter2.1.1 bug on excel file
        // have to check by extension
        // as the mimetype stored in the jodconv. jar is wrong
        // application/application/vnd.ms-excel
        // remove this test when the jodconv. is corrected
        if (mimetypeStr.equals("application/vnd.ms-excel")) {
            format = getSourceFormatByExtension("xls");
        } else {
            format = formatRegistry.getFormatByMimeType(mimetypeStr);
        }
        return format;
    }

    /**
     * Returns the DocumentFormat for the given mimetype.
     *
     * @param mimetype
     * @return DocumentFormat for the given mimetype
     * @throws Exception
     */
    private static DocumentFormat getSourceFormat(String mimetype) {
        return formatRegistry.getFormatByMimeType(mimetype);
    }

    private static DocumentFormat getSourceFormatByExtension(String extension) {
        return formatRegistry.getFormatByFileExtension(extension);
    }

    public String getOOoHostURL() {
        if (descriptor.getParameters().containsKey("ooo_host_name")) {
            String host =  (String) descriptor.getParameters().get("ooo_host_name");
            if (host.trim().startsWith("${")) {
                // for Tests
                return DEFAULT_OOO_HOST_URL;
            }
            else {
                return host.trim();
            }
        } else {
            return DEFAULT_OOO_HOST_URL;
        }
    }

    public int getOOoHostPort() {
        if (descriptor.getParameters().containsKey("ooo_host_port")) {
            try {
                return Integer.parseInt((String) descriptor.getParameters().get("ooo_host_port"));
            }
            catch (NumberFormatException e) {
                return DEFAULT_OOO_HOST_PORT;
            }
        } else {
            return DEFAULT_OOO_HOST_PORT;
        }
    }

    public OpenOfficeConnection getOOoConnection() {
        log.debug("OOo connection lock ACQUIRED");
        if (connection == null || !connection.isConnected()) {
            connection = new SocketOpenOfficeConnection(getOOoHostURL(),
                    getOOoHostPort());
            if (connection.isConnected()) {
                log.info("New Open Office connection established !");
            }
        }
        return connection;
    }

    public void releaseOOoConnection() {
        if (connection != null && connection.isConnected()) {
            connection.disconnect();
        }
    }

    public OpenOfficeDocumentConverter getOOoDocumentConverter()
            throws Exception {
        if (connection != null && connection.isConnected()) {
            return new OpenOfficeDocumentConverter(connection);
        }
        return null;
    }

    @Override
    protected void finalize() throws Throwable {
        releaseOOoConnection();
        super.finalize();
    }

    private void acquireLock() {
        boolean acquired = false;
        try {
            acquired = conLock.tryLock(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Cannot acquire an OOo connection");
        } finally {
            if (!acquired) {
                log.error("Cannot acquire an OOo connection :: timeout");
            }
        }
    }

    private void releaseLock() {
        conLock.unlock();
        log.debug("Release connection lock");
    }

    private Boolean adaptFilterNameForHTML2PDF(DocumentFormat sourceFormat,
            DocumentFormat destinationFormat) {

        // TODO: solve this
        // due to a random bug, we have to be strict regarding otuput FilterName
        // html file have to use "writer_web_pdf_Export" instead of JODConverter
        // simplification "writer_pdf_Export"
        // patch dynamically
        // if (sourceFormat.getMimeType().equals("text/html") &&
        // destinationFormat.getMimeType().equals("application/pdf")) {
        if ("text/html".equals(sourceFormat.getMimeType())
                && "application/pdf".equals(destinationFormat.getMimeType())) {
            // change the FilterName
            DocumentFamily family = sourceFormat.getFamily();
            // Map<String, Serializable> exportOptions =
            // destinationFormat.getExportOptions();
            // exportOptions.put("FilterName", "writer_web_pdf_Export");

            // destinationFormat.setExportOption("FilterName",
            // "writer_web_pdf_Export");

            // String exportFilter = destinationFormat.getExportFilter(family);

            destinationFormat.setExportFilter(family, "writer_web_pdf_Export");

            return true;
        }

        return false;
    }

    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        Blob inputBlob=null;
        try {
            inputBlob = blobHolder.getBlob();
        }
        catch (Exception e) {
            log.error("Error while getting Blob",e);
            throw new ConversionException("Error while getting Blob", e);
        }

         try {


             if (inputBlob==null) {
                 return null;
             }

             // Acquire connection lock.
             acquireLock();

             getOOoConnection();

             // This plugin do deal only with one input source.
             String sourceMimetype = inputBlob.getMimeType();
             try {
                 if (connection != null) {
                     connection.connect();
                 }
             } catch (ConnectException e) {
                 log.error("Could not connect to the remote OpenOffice server @"
                         + getOOoHostURL() + ':' + getOOoHostPort());
                 throw new ConversionException("Could not connect to the remote OpenOffice server @"
                         + getOOoHostURL() + ':' + getOOoHostPort(), e);
             }

             if (connection != null && connection.isConnected()) {
                 File sourceFile = null;
                 File outFile = null;
                 try {

                     // Copy in a file to be able to read it several time
                     sourceFile = File.createTempFile(
                             "NXJOOoConverterDocumentIn", ".bin");
                     InputStream stream = inputBlob.getStream();
                     //if (stream.markSupported()) {
                     //    stream.reset(); // works on a JCRBlobInputStream
                     //}
                     FileUtils.copyToFile(stream, sourceFile);
                     DocumentFormat sourceFormat = null;

                     // TODO: JODconverter2.1.1 bug on excel file
                     // have to check by extension
                     // as the mimetype stored in the jodconv. jar is wrong
                     // application/application/vnd.ms-excel
                     // remove this test when the jodconv. is corrected

                     /*
                      * initial code if (sources[0].getMimetype() != null) { //
                      * Try to fetch it from the registry. //sourceFormat =
                      * getSourceFormat(sources[0].getMimetype()); //sourceFormat =
                      * getSourceFormat("application/application/vnd.ms-excel"); }
                      */

                     if (sourceMimetype != null) {
                         if (sourceMimetype.equals("application/vnd.ms-excel")) {
                             sourceFormat = getSourceFormatByExtension("xls");
                         } else {
                             // Try to fetch it from the registry.
                             sourceFormat = getSourceFormat(sourceMimetype);
                         }
                     }

                     // If not found in the registry or not given as a parameter.
                     // Try to sniff ! What does that smell ? :)
                     if (sourceFormat == null) {
                         sourceFormat = getSourceFormat(sourceFile);
                     }

                     // From plugin settings because we know the destination
                     // mimetype.
                     DocumentFormat destinationFormat = getDestinationFormat();

                     // allow HTML2PDF filtering
                     adaptFilterNameForHTML2PDF(sourceFormat, destinationFormat);

                     outFile = File.createTempFile("NXJOOoConverterDocumentOut",
                             '.' + destinationFormat.getFileExtension());

                     // Perform the actual conversion.
                     getOOoDocumentConverter().convert(sourceFile, sourceFormat,
                             outFile, destinationFormat);

                     // load the content in the file since it will be deleted
                     // soon: TODO: find a way to stream it to the streaming
                     // server without loading it all in memory
                     Blob blob = StreamingBlob.createFromByteArray(
                             new FileSource(outFile).getBytes(),
                             getDestinationMimeType());
                     return new SimpleCachableBlobHolder(blob);

                 } catch (Exception e) {
                     log.error(String.format(
                             "An error occured trying to convert a file to from %s to %s: %s",
                             sourceMimetype, getDestinationMimeType(), e.getMessage()), e);
                     throw new ConversionException("Error in JODConverter", e);
                 } finally {
                     releaseOOoConnection();
                     if (sourceFile != null) {
                         sourceFile.delete();
                     }
                     if (outFile != null) {
                         outFile.delete();
                     }
                 }
             }
             else {
                 throw new ConversionException("Could not connect to the remote OpenOffice server @"
                         + getOOoHostURL() + ':' + getOOoHostPort());
             }
         } finally {
             releaseLock();
         }
    }


    public void init(ConverterDescriptor descriptor) {
        this.descriptor=descriptor;

    }

}
