/*
 * (C) Copyright 2012-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     ldoguin
 */
package org.nuxeo.template.deckjs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableFile;
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

public class DeckJSPDFConverter implements Converter {

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {
        CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
        CommandAvailability commandAvailability = cles.getCommandAvailability(DeckJSConverterConstants.PHANTOM_JS_COMMAND_NAME);
        if (!commandAvailability.isAvailable()) {
            return null;
        }
        Blob blob = blobHolder.getBlob();
        File jsFile = null;
        try (CloseableFile inputFile = blob.getCloseableFile(".html")) {
            jsFile = Framework.createTempFile("phantomJsScript", ".js");
            try (InputStream is = TemplateBundleActivator.getResourceAsStream(DeckJSConverterConstants.DECK_JS2PDF_JS_SCRIPT_PATH)) {
                Files.copy(is, jsFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            Blob pdfOutput = Blobs.createBlobWithExtension(".pdf");
            pdfOutput.setMimeType("application/pdf");
            CmdParameters params = cles.getDefaultCmdParameters();
            params.addNamedParameter("jsFilePath", jsFile);
            params.addNamedParameter("inFilePath", inputFile.getFile());
            params.addNamedParameter("outFilePath", pdfOutput.getFile());
            ExecResult res = cles.execCommand("phantomjs", params);
            if (!res.isSuccessful()) {
                throw res.getError();
            }
            return new SimpleCachableBlobHolder(pdfOutput);
        } catch (CommandNotAvailable | IOException | CommandException e) {
            throw new ConversionException("PDF conversion failed", e);
        } finally {
            org.apache.commons.io.FileUtils.deleteQuietly(jsFile);
        }
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
    }

}
