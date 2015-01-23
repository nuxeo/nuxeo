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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.core.storage.StorageBlob;
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
        File jsFile = null;
        try {
            CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
            CommandAvailability commandAvailability = cles.getCommandAvailability(DeckJSConverterConstants.PHANTOM_JS_COMMAND_NAME);
            if (!commandAvailability.isAvailable()) {
                return null;
            }
            Blob blob = blobHolder.getBlob();
            File inputFile = null;
            if (blob instanceof FileBlob) {
                inputFile = blob.getFile();
            } else if (blob instanceof StorageBlob) {
                inputFile = blob.getFile();
            } else if (blob instanceof StringBlob) {
                inputFile = File.createTempFile("deckJsSource", ".html");
                Framework.trackFile(inputFile, this);
                FileUtils.writeFile(inputFile, blob.getString());
            }
            if (inputFile == null) {
                return null;
            }

            InputStream is = Activator.getResourceAsStream(DeckJSConverterConstants.DECK_JS2PDF_JS_SCRIPT_PATH);
            jsFile = File.createTempFile("phantomJsScript", ".js");
            FileWriter fw = new FileWriter(jsFile);
            IOUtils.copy(is, fw);
            fw.flush();
            fw.close();
            is.close();
            CmdParameters params = new CmdParameters();
            File outputFile = File.createTempFile("nuxeodeckjsPDFrendition", ".pdf");

            params.addNamedParameter("inFilePath", inputFile);
            params.addNamedParameter("outFilePath", outputFile);
            params.addNamedParameter("jsFilePath", jsFile);
            ExecResult res = cles.execCommand("phantomjs", params);
            if (!res.isSuccessful()) {
                throw res.getError();
            }
            Blob pdfOutput = new FileBlob(outputFile);
            pdfOutput.setMimeType("application/pdf");
            String filename = FileUtils.getFileNameNoExt(blob.getFilename());
            filename = filename + ".pdf";
            pdfOutput.setFilename(filename);
            Framework.trackFile(outputFile, pdfOutput);
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
