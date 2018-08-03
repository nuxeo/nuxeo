/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.webengine.model.io;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.io.download.BufferingServletOutputStream;
import org.nuxeo.ecm.core.io.download.DownloadHelper;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Writer for a {@link DocumentBlobHolder}, keeping the context of the current document, which allows for later
 * filtering by the {@link DownloadService}.
 *
 * @since 9.3
 */
@Provider
@Produces({ "*/*", "text/plain" })
public class DocumentBlobHolderWriter implements MessageBodyWriter<DocumentBlobHolder> {

    @Context
    protected HttpServletRequest request;

    @Context
    protected HttpServletResponse response;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return DocumentBlobHolder.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(DocumentBlobHolder blobHolder, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        long n = blobHolder.getBlob().getLength();
        return n < 0 ? -1 : n;
    }

    @Override
    public void writeTo(DocumentBlobHolder blobHolder, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException {
        // ensure transaction is committed before writing blob to response
        commitAndReopenTransaction();
        Blob blob = blobHolder.getBlob();
        // we don't want JAX-RS default headers (like Content-Type: text/plain)
        // to be written, we control everything from the DownloadService
        httpHeaders.clear();
        if (Framework.isTestModeSet()) {
            // TODO remove this test-specific code
            String filename = blob.getFilename();
            if (filename != null) {
                String contentDisposition = DownloadHelper.getRFC2231ContentDisposition(request, filename);
                response.setHeader("Content-Disposition", contentDisposition);
            }
            response.setContentType(blob.getMimeType());
            if (blob.getEncoding() != null) {
                try {
                    response.setCharacterEncoding(blob.getEncoding());
                } catch (IllegalArgumentException e) {
                    // ignore invalid encoding
                }
            }
            transferBlob(blob, entityStream);
            return;
        }
        DocumentModel doc = blobHolder.getDocument();
        String xpath = blobHolder.getXpath();
        DownloadService downloadService = Framework.getService(DownloadService.class);
        downloadService.downloadBlob(request, response, doc, xpath, blob, blob.getFilename(), "download");
    }

    protected void commitAndReopenTransaction() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    protected void transferBlob(Blob blob, OutputStream entityStream) throws IOException {
        if (entityStream instanceof BufferingServletOutputStream) {
            ((BufferingServletOutputStream) entityStream).stopBuffering();
        }
        blob.transferTo(entityStream);
        entityStream.flush();
    }

}
