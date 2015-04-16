/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import javax.servlet.ServletException;
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
import org.nuxeo.ecm.platform.ui.web.download.DownloadServlet;
import org.nuxeo.ecm.platform.web.common.requestcontroller.filter.BufferingServletOutputStream;
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
    public void writeTo(Blob t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException {
        // Ensure transaction is committed before writing blob to response
        commitAndReopenTransaction();
        if (Framework.isTestModeSet()) {
            transferBlob(t, entityStream);
        } else {
            try {
                DownloadServlet.downloadBlob(request, response, t, t.getFilename());
            } catch (ServletException e) {
                log.error("Error while downloading blob", e);
            }
        }

    }

    protected void commitAndReopenTransaction() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    protected void transferBlob(Blob blob, OutputStream entityStream) throws IOException {
        BufferingServletOutputStream.stopBufferingThread();
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
