/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model.io;

import static org.apache.commons.logging.LogFactory.getLog;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.commons.logging.Log;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.JSONBlob;
import org.nuxeo.ecm.core.io.download.BufferingServletOutputStream;
import org.nuxeo.ecm.core.io.download.DownloadHelper;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Provider
@Produces({ "*/*", "text/plain" })
public class BlobWriter implements MessageBodyWriter<Blob> {

    private static final Log log = getLog(BlobWriter.class);

    public static final String BLOB_ID = "blobId";

    @Context
    private HttpServletRequest request;

    @Context
    private HttpServletResponse response;

    @Context
    private ServletContext servletContext;

    @Override
    public void writeTo(Blob blob, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException {
        // Ensure transaction is committed before writing blob to response
        commitAndReopenTransaction();
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
        } else {
            DownloadService downloadService = Framework.getService(DownloadService.class);
            String reason = blob instanceof JSONBlob ? "webengine" : "download";
            downloadService.downloadBlob(request, response, null, null, blob, blob.getFilename(), reason);
        }
    }

    protected void commitAndReopenTransaction() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    protected void transferBlob(Blob blob, OutputStream entityStream) throws IOException {
        if (entityStream instanceof BufferingServletOutputStream) {
            ((BufferingServletOutputStream)entityStream).stopBuffering();
        }
        blob.transferTo(entityStream);
        entityStream.flush();
    }

    @Override
    public long getSize(Blob arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
        long n = arg0.getLength();
        return n <= 0 ? -1 : n;
    }

    @Override
    public boolean isWriteable(Class<?> arg0, Type type, Annotation[] arg2, MediaType arg3) {
        return Blob.class.isAssignableFrom(arg0);
    }

}
