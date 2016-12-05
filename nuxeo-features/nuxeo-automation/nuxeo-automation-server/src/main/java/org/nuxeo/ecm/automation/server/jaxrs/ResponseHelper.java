/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.automation.server.jaxrs;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.core.util.Paginable;
import org.nuxeo.ecm.automation.core.util.RecordSet;
import org.nuxeo.ecm.automation.jaxrs.DefaultJsonAdapter;
import org.nuxeo.ecm.automation.jaxrs.JsonAdapter;
import org.nuxeo.ecm.automation.jaxrs.io.documents.MultipartBlobs;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentRefList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class ResponseHelper {

    private ResponseHelper() {
    }

    public static Response notFound() {
        return Response.status(404).build();
    }

    public static Response emptyContent() {
        return Response.status(204).build();
    }

    public static Response emptyBlobs() {
        return Response.status(204).type("application/nuxeo-empty-list").build();
    }

    public static Response notAllowed() {
        return Response.status(401).build();
    }

    public static Response blob(Blob blob) {
        return blob(blob, HttpServletResponse.SC_OK);
    }

    public static Response blobs(List<Blob> blobs) throws MessagingException, IOException {
        return blobs(blobs, HttpServletResponse.SC_OK);
    }

    public static Response blob(Blob blob, int httpStatus) {
        String type = blob.getMimeType();
        if (type == null || "???".equals(type)) {
            type = MediaType.APPLICATION_OCTET_STREAM;
        }
        if (blob.getEncoding() != null) {
            type += "; charset=" + blob.getEncoding();
        }
        return Response.status(httpStatus)
                       .entity(blob)
                       .type(type)
                       .header("Content-Disposition", "attachment; filename=" + blob.getFilename())
                       .build();
    }

    public static Response blobs(List<Blob> blobs, int httpStatus) throws MessagingException, IOException {
        if (blobs.isEmpty()) {
            return emptyBlobs();
        }
        MultipartBlobs multipartBlobs = new MultipartBlobs(blobs);
        return Response.status(httpStatus)
                       .entity(multipartBlobs)
                       .type(new BoundaryMediaType(multipartBlobs.getContentType()))
                       .build();
    }

    /**
     * @since 5.7.2
     */
    public static Object getResponse(Object result, HttpServletRequest request) throws MessagingException, IOException {
        return getResponse(result, request, HttpServletResponse.SC_OK);
    }

    /**
     * Handle custom http status.
     *
     * @since 7.1
     */
    public static Object getResponse(Object result, HttpServletRequest request, int httpStatus)
            throws IOException, MessagingException {
        if (result == null || "true".equals(request.getHeader("X-NXVoidOperation"))) {
            return emptyContent();
        }
        if (result instanceof Blob) {
            return blob((Blob) result);
        } else if (result instanceof BlobList) {
            return blobs((BlobList) result);
        } else if (result instanceof DocumentRef) {
            CoreSession session = SessionFactory.getSession(request);
            return Response.status(httpStatus).entity(session.getDocument((DocumentRef) result)).build();
        } else if (result instanceof DocumentRefList) {
            CoreSession session = SessionFactory.getSession(request);
            return Response.status(httpStatus).entity(((DocumentRefList) result).stream().map(session::getDocument)
                    .collect(Collectors.toCollection(DocumentModelListImpl::new))).build();
        } else if (result instanceof DocumentModel || result instanceof DocumentModelList
                || result instanceof JsonAdapter || result instanceof RecordSet || result instanceof Paginable<?>) {
            return Response.status(httpStatus).entity(result).build();
        } else { // try to adapt to JSON
            return Response.status(httpStatus).entity(new DefaultJsonAdapter(result)).build();
        }
    }

    /**
     * @since 7.1
     */
    public static class BoundaryMediaType extends MediaType {
        private final String ctype;

        BoundaryMediaType(String ctype) {
            super("multipart", "mixed");
            this.ctype = ctype;
        }

        @Override
        public String toString() {
            return ctype;
        }
    }
}
