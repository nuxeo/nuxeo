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
 *     ldoguin
 */
package org.nuxeo.template.deckjs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import org.nuxeo.common.Environment;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.template.jaxrs.context.JAXRSExtensions;

@Operation(id = DeckJSPDFOperation.ID, category = Constants.CAT_CONVERSION, label = "Convert a deckJS slide to a pdf", description = "Convert a deckJS slide to a pdf.")
public class DeckJSPDFOperation {

    public static final String ID = "Blob.DeckJSToPDF";

    @Context
    OperationContext ctx;

    @Context
    ConversionService conversionService;

    @OperationMethod
    public Blob run(Blob blob) throws IOException {
        DocumentModel templateSourceDocument = (DocumentModel) ctx.get("templateSourceDocument");
        DocumentModel templateBasedDocument = (DocumentModel) ctx.get("templateBasedDocument");
        String templateName = (String) ctx.get("templateName");

        File workingDir = new File(Environment.getDefault().getTemp(), "nuxeo-deckJS-cache/"
                + templateBasedDocument.getId());
        workingDir.mkdirs();
        JAXRSExtensions jaxRsExtensions = new JAXRSExtensions(templateBasedDocument, null, templateName);
        BlobHolder sourceBh = templateSourceDocument.getAdapter(BlobHolder.class);
        for (Blob b : sourceBh.getBlobs()) {
            writeToTempDirectory(workingDir, b);
        }
        BlobHolder templatebasedBh = templateBasedDocument.getAdapter(BlobHolder.class);
        for (Blob b : templatebasedBh.getBlobs()) {
            writeToTempDirectory(workingDir, b);
        }

        String content = blob.getString();
        String resourcePath = jaxRsExtensions.getResourceUrl("");
        content = content.replaceAll(resourcePath, "./");
        File index = new File(workingDir, blob.getFilename());
        FileWriter fw = new FileWriter(index);
        IOUtils.write(content, fw);
        fw.flush();
        fw.close();

        Blob indexBlob = Blobs.createBlob(index);
        indexBlob.setFilename(blob.getFilename());
        Blob result = conversionService.convert("deckJSToPDF", indexBlob, null);
        FileUtils.deleteDirectory(workingDir);
        return result;
    }

    private void writeToTempDirectory(File workingDir, Blob b) throws IOException {
        File f = new File(workingDir, b.getFilename());
        File parentFile = f.getParentFile();
        parentFile.mkdirs();
        b.transferTo(f);
    }
}
