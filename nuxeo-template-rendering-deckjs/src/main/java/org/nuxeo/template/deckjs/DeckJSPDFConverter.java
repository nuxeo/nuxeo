/*
 * (C) Copyright 2012-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import org.nuxeo.ecm.core.api.ClientException;
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
        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
        CommandAvailability commandAvailability = cles.getCommandAvailability(DeckJSConverterConstants.PHANTOM_JS_COMMAND_NAME);
        if (!commandAvailability.isAvailable()) {
            return null;
        }
        Blob blob = blobHolder.getBlob();
        File jsFile = null;
        try (CloseableFile inputFile = blob.getCloseableFile(".html")) {
            jsFile = File.createTempFile("phantomJsScript", ".js");
            try (InputStream is = Activator.getResourceAsStream(DeckJSConverterConstants.DECK_JS2PDF_JS_SCRIPT_PATH)) {
                Files.copy(is, jsFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            Blob pdfOutput = Blobs.createBlobWithExtension(".pdf");
            pdfOutput.setMimeType("application/pdf");
            CmdParameters params = new CmdParameters();
            params.addNamedParameter("jsFilePath", jsFile);
            params.addNamedParameter("inFilePath", inputFile.getFile());
            params.addNamedParameter("outFilePath", pdfOutput.getFile());
            ExecResult res = cles.execCommand("phantomjs", params);
            if (!res.isSuccessful()) {
                throw res.getError();
            }
            return new SimpleCachableBlobHolder(pdfOutput);
        } catch (CommandNotAvailable | IOException | ClientException | CommandException e) {
            throw new ConversionException("PDF conversion failed", e);
        } finally {
            org.apache.commons.io.FileUtils.deleteQuietly(jsFile);
        }
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
    }

}
