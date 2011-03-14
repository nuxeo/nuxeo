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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.artofsolving.jodconverter.document.DefaultDocumentFormatRegistry;
import org.artofsolving.jodconverter.document.DocumentFamily;
import org.artofsolving.jodconverter.document.DocumentFormat;
import org.artofsolving.jodconverter.document.DocumentFormatRegistry;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.core.convert.extension.ExternalConverter;
import org.nuxeo.ecm.platform.convert.ooomanager.OOoManagerService;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.streaming.FileSource;

import com.sun.star.uno.RuntimeException;

public class JODBasedConverter implements ExternalConverter {

    protected static final String TMP_PATH_PARAMETER = "TmpDirectory";

    private static final Log log = LogFactory.getLog(JODBasedConverter.class);

    private static final DocumentFormatRegistry formatRegistry = new DefaultDocumentFormatRegistry();

    protected ConverterDescriptor descriptor;

    protected String getDestinationMimeType() {
        return descriptor.getDestinationMimeType();
    }

    /**
     * Returns the destination format for the given plugin.
     * <p>
     * It takes the actual destination mimetype from the plugin configuration.
     *
     * @return the DestinationFormat for this given plugin. {@see
     *         org.nuxeo.ecm.platform.transform.interfaces.Plugin}
     */
    private DocumentFormat getDestinationFormat() {
        return formatRegistry.getFormatByMediaType(getDestinationMimeType());
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
        MimetypeRegistry mimetypeRegistry = Framework.getService(MimetypeRegistry.class);
        String mimetypeStr = mimetypeRegistry.getMimetypeFromFile(file);
        DocumentFormat format = formatRegistry.getFormatByMediaType(mimetypeStr);
        return format;
    }

    /**
     * Returns the DocumentFormat for the given mimetype.
     *
     * @return DocumentFormat for the given mimetype
     */
    private static DocumentFormat getSourceFormat(String mimetype) {
        return formatRegistry.getFormatByMediaType(mimetype);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    private static boolean adaptFilterNameForHTML2PDF(DocumentFormat sourceFormat,
            DocumentFormat destinationFormat) {

        // TODO: solve this
        // due to a random bug, we have to be strict regarding otuput FilterName
        // html file have to use "writer_web_pdf_Export" instead of JODConverter
        // simplification "writer_pdf_Export"
        // patch dynamically
        if ("text/html".equals(sourceFormat.getMediaType())
                && "application/pdf".equals(destinationFormat.getMediaType())) {
            // change the FilterName
            DocumentFamily family = sourceFormat.getInputFamily();
            Map<String, String> storeProperties = new HashMap<String, String>();
            storeProperties.put("FilterName", "writer_web_pdf_Export");
            destinationFormat.setStoreProperties(family, storeProperties);
            return true;
        }
        return false;
    }

    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {
        Blob inputBlob;
        OfficeDocumentConverter documentConverter;
        try {
            inputBlob = blobHolder.getBlob();
            OOoManagerService oooManagerService = Framework.getService(OOoManagerService.class);
            documentConverter = oooManagerService.getDocumentConverter();
        } catch (Exception e) {
            throw new ConversionException("Error while getting Blob", e);
        }

        if (inputBlob == null) {
            return null;
        }
        // This plugin do deal only with one input source.
        String sourceMimetype = inputBlob.getMimeType();

        if (documentConverter != null) {
            File sourceFile = null;
            File outFile = null;
            File[] files = null;
            try {

                // Get original file extension
                String ext = inputBlob.getFilename();
                int dotPosition = ext.lastIndexOf('.');
                if (dotPosition == -1) {
                    ext = ".bin";
                } else {
                    ext = ext.substring(dotPosition);
                }
                // Copy in a file to be able to read it several time
                sourceFile = File.createTempFile("NXJOOoConverterDocumentIn",
                        ext);
                InputStream stream = inputBlob.getStream();
                // if (stream.markSupported()) {
                // stream.reset(); // works on a JCRBlobInputStream
                // }
                FileUtils.copyToFile(stream, sourceFile);
                DocumentFormat sourceFormat = null;

                if (sourceMimetype != null) {
                    // Try to fetch it from the registry.
                    sourceFormat = getSourceFormat(sourceMimetype);
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
                    File myTmpDir = new File(tmpDirPath + "/JODConv_"
                            + System.currentTimeMillis());
                    boolean created = myTmpDir.mkdir();
                    if (!created) {
                        throw new ConversionException(
                                "Unable to create temp dir");
                    }

                    outFile = new File(myTmpDir.getAbsolutePath() + "/"
                            + "NXJOOoConverterDocumentOut."
                            + destinationFormat.getExtension());

                    created = outFile.createNewFile();
                    if (!created) {
                        throw new ConversionException(
                                "Unable to create temp file");
                    }

                    log.debug("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
                    log.debug("Input File = " + outFile.getAbsolutePath());
                    // Perform the actual conversion.
                    documentConverter.convert(sourceFile, outFile,
                            destinationFormat);

                    files = myTmpDir.listFiles();
                    for (File file : files) {
                        Blob blob = StreamingBlob.createFromByteArray(new FileSource(
                                file).getBytes());
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
                            '.' + destinationFormat.getExtension());

                    // Perform the actual conversion.
                    documentConverter.convert(sourceFile, outFile,
                            destinationFormat);

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
                log.error(
                        String.format(
                                "An error occurred trying to convert a file to from %s to %s: %s",
                                sourceMimetype, getDestinationMimeType(),
                                e.getMessage()), e);
                throw new ConversionException("Error in JODConverter", e);
            } finally {
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
            throw new ConversionException(
                    "Could not connect to the remote OpenOffice server");
        }

    }

    public void init(ConverterDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public ConverterCheckResult isConverterAvailable() {
        ConverterCheckResult result = new ConverterCheckResult();
        try {
            OOoManagerService oooManagerService = Framework.getService(OOoManagerService.class);
            if (!oooManagerService.isOOoManagerStarted()) {
                result.setAvailable(false);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not get OOoManagerService");
        }
        return result;
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
