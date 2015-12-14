/*
 * Copyright (c) 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandException;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.picture.api.BlobHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Converter bean managing the thumbnail conversion for picture documents
 *
 * @since 5.7
 */
public class ThumbnailDocumentConverter implements Converter {

    public static final String THUMBNAIL_CONVERTER_NAME = "toThumbnail";

    public static final String THUMBNAIL_SIZE_PARAMETER_NAME = "size";

    public static final String THUMBNAIL_DEFAULT_SIZE = "100x100";

    public static final String THUMBNAIL_COMMAND = "toThumbnail";

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {
        File tmpFile = null;
        try {
            // Make sure the toThumbnail command is available
            CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
            CommandAvailability commandAvailability = cles.getCommandAvailability(THUMBNAIL_COMMAND);
            if (!commandAvailability.isAvailable()) {
                return null;
            }
            // get the input and output of the command
            Blob blob = blobHolder.getBlob();
            blob.persist();

            File inputFile = BlobHelper.getFileFromBlob(blob);
            if (inputFile == null) {
                tmpFile = File.createTempFile("nuxeoImageInfo", ".tmp");
                blob.transferTo(tmpFile);
                inputFile = tmpFile;
            }

            CmdParameters params = new CmdParameters();
            File outputFile = File.createTempFile("nuxeoImageTarget", "." + "png");
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
                throw res.getError();
            }
            Blob targetBlob = new FileBlob(outputFile);
            Framework.trackFile(outputFile, targetBlob);
            return new SimpleCachableBlobHolder(targetBlob);
        } catch (CommandNotAvailable | IOException | ClientException | CommandException e) {
            throw new ConversionException("Thumbnail conversion failed", e);
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
    }
}
