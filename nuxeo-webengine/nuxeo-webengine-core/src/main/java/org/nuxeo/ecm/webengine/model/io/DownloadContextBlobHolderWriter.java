/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
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

import org.nuxeo.ecm.core.api.blobholder.DownloadContextBlobHolder;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.io.download.DownloadService.DownloadContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Writer for a {@link DownloadContextBlobHolder}, keeping the original document context, which allows for later
 * filtering on document permissions by the {@link DownloadService}.
 *
 * @since 2021.25
 */
@Provider
@Produces({ "*/*", "text/plain" })
public class DownloadContextBlobHolderWriter implements MessageBodyWriter<DownloadContextBlobHolder> {

    @Context
    protected HttpServletRequest request;

    @Context
    protected HttpServletResponse response;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return DownloadContextBlobHolder.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(DownloadContextBlobHolder blobHolder, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        long n = blobHolder.getBlob().getLength();
        return n < 0 ? -1 : n;
    }

    @Override
    public void writeTo(DownloadContextBlobHolder blobHolder, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException {
        // ensure transaction is committed before writing blob to response
        commitAndReopenTransaction();
        // we don't want JAX-RS default headers
        httpHeaders.clear();
        DownloadContext context = DownloadContext.builder(request, response)
                                                 .doc(blobHolder.getDocument())
                                                 .blob(blobHolder.getBlob())
                                                 .filename(blobHolder.getFilename())
                                                 .reason(blobHolder.getReason())
                                                 .extendedInfos(blobHolder.getExtendedInfos())
                                                 .inline(blobHolder.isInline())
                                                 .lastModified(blobHolder.getModificationDate())
                                                 .build();
        Framework.getService(DownloadService.class).downloadBlob(context);
    }

    protected void commitAndReopenTransaction() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

}
