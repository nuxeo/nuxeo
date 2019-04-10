/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ldoguin
 */
package org.nuxeo.template.deckjs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.template.jaxrs.context.JAXRSExtensions;

@Operation(id = DeckJSPDFOperation.ID, category = Constants.CAT_CONVERSION, label = "Convert a deckJS slide to a pdf", description = "Convert a deckJS slide to a pdf.")
public class DeckJSPDFOperation {

    public static final String ID = "Blob.DeckJSToPDF";

    @Context
    OperationContext ctx;

    @Context
    ConversionService conversionService;

    @OperationMethod
    public Blob run(Blob blob) throws Exception {
        DocumentModel templateSourceDocument = (DocumentModel) ctx.get("templateSourceDocument");
        DocumentModel templateBasedDocument = (DocumentModel) ctx.get("templateBasedDocument");
        String templateName = (String) ctx.get("templateName");

        String workingDirPath = System.getProperty("java.io.tmpdir") + "/nuxeo-deckJS-cache/"
                + templateBasedDocument.getId();
        File workingDir = new File(workingDirPath);
        if (!workingDir.exists()) {
            workingDir.mkdirs();
        }
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
        BlobHolder bh = conversionService.convert("deckJSToPDF", new SimpleCachableBlobHolder(indexBlob), null);
        FileUtils.deleteDirectory(workingDir);
        return bh.getBlob();
    }

    private void writeToTempDirectory(File workingDir, Blob b) throws IOException {
        File f = new File(workingDir, b.getFilename());
        File parentFile = f.getParentFile();
        parentFile.mkdirs();
        b.transferTo(f);
    }
}
