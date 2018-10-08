/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 * Laurent Doguin <ldoguin@nuxeo.com>
 * Vladimir Pasquier <vpasquier@nuxeo.com>
 *
 */
package org.nuxeo.ecm.platform.thumbnail.converter;

import static org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants.THUMBNAIL_DEFAULT_SIZE;
import static org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants.THUMBNAIL_SIZE_PARAMETER_NAME;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
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
import org.nuxeo.runtime.api.Framework;

/**
 * Converter bean managing the thumbnail conversion for picture documents
 *
 * @since 5.7
 */
public class ThumbnailDocumentConverter implements Converter {

    public static final String THUMBNAIL_COMMAND = "toThumbnail";

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) {
        try {
            // Make sure the toThumbnail command is available
            CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
            CommandAvailability commandAvailability = cles.getCommandAvailability(THUMBNAIL_COMMAND);
            if (!commandAvailability.isAvailable()) {
                return null;
            }
            // get the input and output of the command
            Blob blob = blobHolder.getBlob();

            Blob targetBlob = Blobs.createBlobWithExtension(".png");
            targetBlob.setMimeType("image/png");
            try (CloseableFile source = blob.getCloseableFile()) {
                CmdParameters params = cles.getDefaultCmdParameters();
                String size;
                if (parameters != null && parameters.containsKey(THUMBNAIL_SIZE_PARAMETER_NAME)) {
                    size = (String) parameters.get(THUMBNAIL_SIZE_PARAMETER_NAME);
                } else {
                    size = THUMBNAIL_DEFAULT_SIZE;
                }
                params.addNamedParameter(THUMBNAIL_SIZE_PARAMETER_NAME, size);
                params.addNamedParameter("inputFilePath", source.getFile());
                params.addNamedParameter("outputFilePath", targetBlob.getFile());

                ExecResult res = cles.execCommand(THUMBNAIL_COMMAND, params);
                if (!res.isSuccessful()) {
                    throw res.getError();
                }
            }
            return new SimpleCachableBlobHolder(targetBlob);
        } catch (CommandNotAvailable | IOException | NuxeoException | CommandException e) {
            throw new ConversionException("Thumbnail conversion failed", e);
        }
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
        // Nothing to do here
    }
}
