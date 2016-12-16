/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Tiry
 *     bstefanescu <bs@nuxeo.com>
 *     Estelle Giuly <egiuly@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.blob;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import java.nio.file.Path;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;

/**
 * Save the input document
 */
@Operation(id = BlobToPDF.ID, category = Constants.CAT_CONVERSION, label = "Convert To PDF", description = "Convert the input file to a PDF and return the new file.")
public class BlobToPDF {

    public static final String ID = "Blob.ToPDF";

    @Context
    protected ConversionService service;

    @OperationMethod
    public Blob run(DocumentModel doc) {
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh == null) {
            return null;
        }
        if (MimetypeRegistry.PDF_MIMETYPE.equals(bh.getBlob().getMimeType())) {
            return bh.getBlob();
        }
        BlobHolder pdfBh = service.convertToMimeType(MimetypeRegistry.PDF_MIMETYPE, bh, new HashMap<String, Serializable>());
        Blob result = pdfBh.getBlob();

        String fname = result.getFilename();
        String filename = bh.getBlob().getFilename();
        if (filename != null && !filename.isEmpty()) {
            // add pdf extension
            int pos = filename.lastIndexOf('.');
            if (pos > 0) {
                filename = filename.substring(0, pos);
            }
            filename += ".pdf";
            result.setFilename(filename);
        } else if (fname != null && !fname.isEmpty()) {
            result.setFilename(fname);
        } else {
            result.setFilename("file");
        }

        result.setMimeType(MimetypeRegistry.PDF_MIMETYPE);
        return result;
    }

    @OperationMethod
    public Blob run(Blob blob) throws IOException {
        String mimetype = blob.getMimeType();
        if (MimetypeRegistry.PDF_MIMETYPE.equals(mimetype)) {
            return blob;
        }
        Blob result;
        if (MediaType.TEXT_PLAIN.equals(mimetype)) {
            result = convertBlobToMimeType(blob, MimetypeRegistry.PDF_MIMETYPE);
        } else {
            // Convert the blob to HTML
            if (!MediaType.TEXT_HTML.equals(mimetype)) {
                String filename = blob.getFilename();
                blob = convertBlobToMimeType(blob, MediaType.TEXT_HTML);
                blob.setFilename(filename);
            }
            // Replace the image URLs by absolute paths
            Path tempDirectory = Framework.createTempDirectory("blobs");
            blob = replaceURLsByAbsolutePaths(blob, tempDirectory);
            // Convert the blob to PDF
            result = convertBlobToMimeType(blob, MimetypeRegistry.PDF_MIMETYPE);
            org.apache.commons.io.FileUtils.deleteQuietly(tempDirectory.toFile());
        }
        adjustBlobName(blob, result);
        return result;
    }

    @OperationMethod
    public BlobList run(BlobList blobs) throws IOException {
        BlobList bl = new BlobList();
        for (Blob blob : blobs) {
            bl.add(this.run(blob));
        }
        return bl;
    }

    protected Blob convertBlobToMimeType(Blob blob, String mimetype) {
        BlobHolder bh = new SimpleBlobHolder(blob);
        bh = service.convertToMimeType(mimetype, bh, new HashMap<String, Serializable>());
        Blob result = bh.getBlob();
        return result;
    }

    protected void adjustBlobName(Blob in, Blob out) {
        String fname = in.getFilename();
        if (fname == null) {
            fname = "Unknown_" + System.identityHashCode(in);
        }
        out.setFilename(fname + ".pdf");
        out.setMimeType(MimetypeRegistry.PDF_MIMETYPE);
    }

    /**
     * Replace the image URLs of an HTML blob by absolute local paths
     *
     * @throws IOException
     * @since 9.1
     */
    @SuppressWarnings("unchecked")
    protected Blob replaceURLsByAbsolutePaths(Blob blob, Path tempDirectory) throws IOException {
        String initialBlobContent = blob.getString();
        String blobContentWithAbsolutePaths = initialBlobContent;
        DownloadService downloadService = Framework.getService(DownloadService.class);
        List<Map<String, String>> parsedUrls = downloadService.parseDownloadUrlsInHtmlLinks(initialBlobContent);
        for (Map<String, String> parsedUrl : parsedUrls) {
            try {
                // Retrieve the document
                CoreSession session = CoreInstance.openCoreSession(parsedUrl.get("repository"));
                IdRef docIdRef = new IdRef(parsedUrl.get("id"));
                DocumentModel doc = session.getDocument(docIdRef);

                // Retrieve the image
                List<Map<String, Object>> docFiles = (List<Map<String, Object>>) doc.getPropertyValue("files:files");
                int fileIndex = Integer.parseInt(parsedUrl.get("index"));
                if (docFiles == null || docFiles.size() <= fileIndex) {
                    break;
                }
                Map<String, Object> imageDoc = docFiles.get(fileIndex);
                if (imageDoc == null) {
                    break;
                }
                Blob imageBlob = (Blob) imageDoc.get("file");
                if (imageBlob == null) {
                    break;
                }

                // Export the image to a temporary directory in File System
                String sanitizedFilename = FileUtils.sanitizeFilename((String) imageDoc.get("filename"));
                File imageFile = tempDirectory.resolve(sanitizedFilename).toFile();
                imageBlob.transferTo(imageFile);
                session.close();

                // Replace the image's URL by its absolute local path
                blobContentWithAbsolutePaths = blobContentWithAbsolutePaths.replaceAll(parsedUrl.get("url"),
                        imageFile.toPath().toString());
            } catch (DocumentNotFoundException | DocumentSecurityException | NumberFormatException e) {
                break;
            }
        }
        if (blobContentWithAbsolutePaths.equals(initialBlobContent)) {
            return blob;
        }
        // Create a new blob with the new content
        Blob newBlob = new StringBlob(blobContentWithAbsolutePaths, blob.getMimeType(), blob.getEncoding());
        newBlob.setFilename(blob.getFilename());
        return newBlob;
    }

}
