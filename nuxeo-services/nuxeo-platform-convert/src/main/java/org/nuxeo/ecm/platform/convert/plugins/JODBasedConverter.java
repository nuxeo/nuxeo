/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.convert.plugins;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
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
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.core.convert.extension.ExternalConverter;
import org.nuxeo.ecm.platform.convert.oooserver.OOoDaemonService;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.streaming.FileSource;

import com.artofsolving.jodconverter.DefaultDocumentFormatRegistry;
import com.artofsolving.jodconverter.DocumentFamily;
import com.artofsolving.jodconverter.DocumentFormat;
import com.artofsolving.jodconverter.DocumentFormatRegistry;
import com.artofsolving.jodconverter.openoffice.connection.OpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.converter.OpenOfficeDocumentConverter;

public class JODBasedConverter implements ExternalConverter {

    protected static final String TMP_PATH_PARAMETER = "TmpDirectory";

    private static final Log log = LogFactory.getLog(JODBasedConverter.class);

    private static final DocumentFormatRegistry formatRegistry = new DefaultDocumentFormatRegistry();

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
     * Returns the destination format for the given plugin.
     * <p>
     * It takes the actual destination mimetype from the plugin
     * configuration.
     *
     * @return the DestinationFormat for this given plugin.
     *         {@see org.nuxeo.ecm.platform.transform.interfaces.Plugin}
     */
    private DocumentFormat getDestinationFormat() {
        return formatRegistry.getFormatByMimeType(getDestinationMimeType());
    }

    /**
     * Returns the format for the file passed as a parameter.
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
            String host = (String) descriptor.getParameters().get("ooo_host_name");
            if (host.trim().startsWith("${")) {
                // for Tests
                return DEFAULT_OOO_HOST_URL;
            } else {
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
        OOoDaemonService ods = Framework.getLocalService(OOoDaemonService.class);
        if (ods != null) {
            if (ods.isEnabled()) {
                if (ods.isConfigured()) {
                    if (!ods.isRunning()) {
                        ods.startDaemonAndWaitUntilReady();
                    }
                } else {
                    log.debug("OOoDaemonService is not configured, expect OOo to be already running");
                }
            } else {
                log.debug("OOoDaemonService is not enabled, expect OOo to be already running");
            }
        }

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

    private boolean acquireLock() {
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
        return acquired;
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

        Blob inputBlob;
        try {
            inputBlob = blobHolder.getBlob();
        }
        catch (Exception e) {
            log.error("Error while getting Blob", e);
            throw new ConversionException("Error while getting Blob", e);
        }

        try {


            if (inputBlob == null) {
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
                File[] files = null;
                try {

                    // Get original file extension
                    String ext = inputBlob.getFilename();
                    int dotPosition = ext.lastIndexOf(".");
                    if (dotPosition == -1) {
                        ext = ".bin";
                    } else {
                        ext = ext.substring(dotPosition);
                    }
                    // Copy in a file to be able to read it several time
                    sourceFile = File.createTempFile(
                            "NXJOOoConverterDocumentIn", ext);
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

                    List<Blob> blobs = new ArrayList<Blob>();

                    if (descriptor.getDestinationMimeType().equals("text/html")) {
                        String tmpDirPath = getTmpDirectory();
                        File myTmpDir = new File(tmpDirPath + "/JODConv_" + System.currentTimeMillis());
                        boolean created = myTmpDir.mkdir();
                        if (!created) {
                            throw new ConversionException("Unable to create temp dir");
                        }

                        outFile = new File(myTmpDir.getAbsolutePath() + "/" + "NXJOOoConverterDocumentOut." + destinationFormat.getFileExtension());

                        created = outFile.createNewFile();
                        if (!created) {
                            throw new ConversionException("Unable to create temp file");
                        }

                        log.debug("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
                        log.debug("Input File = " + outFile.getAbsolutePath());
                        // Perform the actual conversion.
                        getOOoDocumentConverter().convert(sourceFile, sourceFormat,
                                outFile, destinationFormat);

                        files = myTmpDir.listFiles();
                        for (File file : files) {
                            Blob blob = StreamingBlob.createFromByteArray(
                                    new FileSource(file).getBytes());
                            if (file.getName().equals(outFile.getName())) {
                                blob.setFilename("index.html");
                                blobs.add(0, blob);
                            } else {
                                blob.setFilename(file.getName());
                                blobs.add(blob);
                            }
                        }

                    } else {
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
                        blobs.add(blob);
                    }

                    return new SimpleCachableBlobHolder(blobs);

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

                    if (files != null) {
                        for (File file : files) {
                            if (file.exists()) {
                                file.delete();
                            }
                        }
                    }
                }
            } else {
                throw new ConversionException("Could not connect to the remote OpenOffice server @"
                        + getOOoHostURL() + ':' + getOOoHostPort());
            }
        } finally {
            releaseLock();
        }
    }

    public void init(ConverterDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public synchronized ConverterCheckResult isConverterAvailable() {
        boolean locked = acquireLock();
        try {
            getOOoConnection();
            connection.connect();
            getOOoDocumentConverter();

            if (connection.isConnected()) {
                return new ConverterCheckResult();
            } else {
                return new ConverterCheckResult("OOo must be running in Listen mode", "Can not open connection");
            }
        }
        catch (Exception e) {
            return new ConverterCheckResult("OOo must be running in Listen mode", e.getMessage());
        }
        finally {
            try {
                releaseOOoConnection();
            }
            finally {
                if (locked) {
                    releaseLock();
                }
            }
        }
    }

    protected String getTmpDirectory() {
        String tmp = null;
        Map<String, String> parameters = descriptor.getParameters();
        if (parameters != null && parameters.containsKey(TMP_PATH_PARAMETER)) {
            tmp = parameters.get(TMP_PATH_PARAMETER);
        }
        if (tmp == null) {
            tmp = System.getProperty("java.io.tmpdir");
        }
        return tmp;
    }

}
