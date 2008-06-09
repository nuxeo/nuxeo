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
 * $Id: JOOoConvertPluginImpl.java 28924 2008-01-10 14:04:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.transform.plugin.joooconverter.impl;

import java.io.File;
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
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.platform.mimetype.NXMimeTypeHelper;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.plugin.AbstractPlugin;
import org.nuxeo.ecm.platform.transform.plugin.joooconverter.api.JOOoConverterPlugin;
import org.nuxeo.ecm.platform.transform.timer.SimpleTimer;
import org.nuxeo.runtime.services.streaming.FileSource;

import com.artofsolving.jodconverter.DocumentFamily;
import com.artofsolving.jodconverter.DocumentFormat;
import com.artofsolving.jodconverter.DocumentFormatRegistry;
import com.artofsolving.jodconverter.XmlDocumentFormatRegistry;
import com.artofsolving.jodconverter.openoffice.connection.OpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.converter.OpenOfficeDocumentConverter;

/**
 * JOOConverterPlugin implementation for TransformServiceCommon.
 * <p>
 * Defines a plugin that can be registred as a TransformServiceCommon plugin
 * extension. This plugin will request an distant OpenOffice server for
 * conversion. It leverages joooconverter the OpenOffice.org java lib.
 * <p>
 * PLEASE CHECK TESTS IF YOU MODIFY THIS PLUGIN. YOU NEED AN UP AND RUNNING OO
 * INSTANCE. RUNNING LOCALLY. YOU CAN LAUNCH IT WITH :
 * <code>openoffice.org -headless -accept="socket,port=8100;urp;"</code>
 *
 * @see org.nuxeo.ecm.platform.transform.plugin.joooconverter.api.JOOoConverterPlugin
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class JOOoConvertPluginImpl extends AbstractPlugin implements
        JOOoConverterPlugin {

    private static final Log log = LogFactory.getLog(JOOoConvertPluginImpl.class);

    private static final long serialVersionUID = 1L;

    private static final DocumentFormatRegistry formatRegistry = new XmlDocumentFormatRegistry();

    private static final String DEFAULT_OOO_HOST_URL = "localhost";

    private static final int DEFAULT_OOO_HOST_PORT = 8100;

    // OOo doesn't support multi thread connection on the same port.
    private static OpenOfficeConnection connection;

    private static final Lock conLock = new ReentrantLock();

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
        MimetypeRegistry mimetypeRegistry = NXMimeTypeHelper.getMimetypeRegistryService();
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
        if (defaultOptions.containsKey("ooo_host_name")) {
            return (String) defaultOptions.get("ooo_host_name");
        } else {
            return DEFAULT_OOO_HOST_URL;
        }
    }

    public int getOOoHostPort() {
        if (defaultOptions.containsKey("ooo_host_port")) {
            return Integer.parseInt((String) defaultOptions.get("ooo_host_port"));
        } else {
            return DEFAULT_OOO_HOST_PORT;
        }
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

    public void releaseLock() {
        conLock.unlock();
        log.debug("Release connection lock");
    }

    @Override
    public List<TransformDocument> transform(Map<String, Serializable> options,
            TransformDocument... sources) throws Exception {

        // Merge transformer options overriding default ones if present.
        List<TransformDocument> results = new ArrayList<TransformDocument>();
        results = super.transform(options, sources);

        // We need at least one source for the transformation.
        if (sources == null || sources.length != 1) {
            log.error("One source is needed for such a transformation");
            return results;
        }

        SimpleTimer timer = new SimpleTimer();
        try {

            timer.start();

            // Acquire connection lock.
            acquireLock();

            getOOoConnection();

            // This plugin do deal only with one input source.
            String sourceMimetype = sources[0].getMimetype();
            try {
                if (connection != null) {
                    connection.connect();
                }
            } catch (ConnectException e) {
                log.error("Could not connect to the remote OpenOffice server @"
                        + getOOoHostURL() + ':'
                        + String.valueOf(getOOoHostPort()));
            }

            if (connection != null && connection.isConnected()) {
                File sourceFile = null;
                File outFile = null;
                try {

                    // Copy in a file to be able to read it several time
                    sourceFile = File.createTempFile(
                            "NXJOOoConverterDocumentIn", ".bin");
                    FileUtils.copyToFile(sources[0].getBlob().getStream(), sourceFile);
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
                            destinationMimeType);
                    results.add(new TransformDocumentImpl(blob));
                } catch (Exception e) {
                    log.error(String.format(
                            "An error occured trying to convert a file to from %s to %s: %s",
                            sourceMimetype, destinationMimeType, e.getMessage()));
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
        } finally {
            releaseLock();
            timer.stop();
            log.debug("Transformation terminated plugin side. " + timer);
        }

        return results;
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

    private Boolean adaptFilterNameForHTML2PDF(DocumentFormat sourceFormat, DocumentFormat destinationFormat){

        // TODO: solve this
        // due to a random bug, we have to be strict regarding otuput FilterName
        // html file have to use "writer_web_pdf_Export" instead of JODConverter simplification "writer_pdf_Export"
        // patch dynamically
        //if (sourceFormat.getMimeType().equals("text/html") && destinationFormat.getMimeType().equals("application/pdf")) {
        if ("text/html".equals(sourceFormat.getMimeType()) && "application/pdf".equals(destinationFormat.getMimeType())) {
            // change the FilterName
                DocumentFamily family = sourceFormat.getFamily();
             //   Map<String, Serializable> exportOptions = destinationFormat.getExportOptions();
             //   exportOptions.put("FilterName", "writer_web_pdf_Export");

          //      destinationFormat.setExportOption("FilterName", "writer_web_pdf_Export");

           //     String exportFilter = destinationFormat.getExportFilter(family);

                destinationFormat.setExportFilter(family, "writer_web_pdf_Export");


                return true;
        }

        return false;
    }

}
