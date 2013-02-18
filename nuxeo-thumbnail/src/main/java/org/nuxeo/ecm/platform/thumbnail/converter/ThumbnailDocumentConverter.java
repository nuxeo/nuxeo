/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Laurent Doguin <ldoguin@nuxeo.com>
 * Vladimir Pasquier <vpasquier@nuxeo.com>
 * 
 */
package org.nuxeo.ecm.platform.thumbnail.converter;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLBlob;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.streaming.FileSource;
import org.nuxeo.runtime.services.streaming.StreamSource;

/**
 * Converter bean managing the thumbnail conversion for picture documents
 * 
 * @since 5.7
 */
public class ThumbnailDocumentConverter implements Converter {

    public static final String THUMBNAIL_CONVERTER_NAME = "toThumbnail";

    public static final String THUMBNAIL_SIZE_PARAMETER_NAME = "size";

    public static final String THUMBNAIL_DEFAULT_SIZE = "100";

    public static final String THUMBNAIL_COMMAND = "toThumbnail";

    @Override
    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {
        try {
            // Make sure the toThumbnail command is available
            CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
            CommandAvailability commandAvailability = cles.getCommandAvailability(THUMBNAIL_COMMAND);
            if (!commandAvailability.isAvailable()) {
                return null;
            }
            // get the input and output of the command
            Blob blob = blobHolder.getBlob();
            File inputFile = null;
            if (blob instanceof FileBlob) {
                inputFile = ((FileBlob) blob).getFile();
            } else if (blob instanceof SQLBlob) {
                StreamSource source = ((SQLBlob) blob).getBinary().getStreamSource();
                inputFile = ((FileSource) source).getFile();
            } else if (blob instanceof StreamingBlob) {
                StreamingBlob streamingBlob = ((StreamingBlob) blob);
                if (!streamingBlob.isPersistent()) {
                    streamingBlob.persist();
                }
                StreamSource source = streamingBlob.getStreamSource();
                inputFile = ((FileSource) source).getFile();
            }
            if (inputFile == null) {
                return null;
            }
            CmdParameters params = new CmdParameters();
            File outputFile = File.createTempFile("nuxeoImageTarget", "."
                    + "png");
            String size = THUMBNAIL_DEFAULT_SIZE;
            if (parameters != null) {
                if (parameters.containsKey(THUMBNAIL_SIZE_PARAMETER_NAME)) {
                    size = (String) parameters.get(THUMBNAIL_SIZE_PARAMETER_NAME);
                }
            }
            params.addNamedParameter(THUMBNAIL_SIZE_PARAMETER_NAME, size);
            params.addNamedParameter("inputFilePath", inputFile);
            params.addNamedParameter("outputFilePath", outputFile);

            ExecResult res = cles.execCommand(THUMBNAIL_COMMAND, params);
            if (!res.isSuccessful()) {
                return null;
            }
            Blob targetBlob = new FileBlob(outputFile);
            Framework.trackFile(outputFile, targetBlob);
            return new SimpleCachableBlobHolder(targetBlob);
        } catch (Exception e) {
            throw new ConversionException("Thumbnail conversion has failed", e);
        }
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
    }
}
