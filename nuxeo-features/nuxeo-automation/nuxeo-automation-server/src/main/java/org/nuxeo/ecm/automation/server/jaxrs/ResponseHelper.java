/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Bogdan Stefanescu
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.automation.server.jaxrs;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
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

    public static Response notAllowed() {
        return Response.status(401).build();
    }

    public static Response blob(Blob blob) {
        String type = blob.getMimeType();
        if (type == null || "???".equals(type)) {
            type = MediaType.APPLICATION_OCTET_STREAM;
        }
        return Response.ok(blob).type(type).header("Content-Disposition",
                "attachment; filename=" + blob.getFilename()).build();
    }

    public static Response blobs(List<Blob> blobs) throws Exception {
        return new MultipartBlobs(blobs).getResponse();
    }

    /**
     * @since 5.7.2
     */
    public static Object getResponse(Object result, HttpServletRequest request)
            throws Exception {
        if (result == null) {
            return null;
        }
        if ("true".equals(request.getHeader("X-NXVoidOperation"))) {
            return emptyContent(); // void response
        }
        if (result instanceof Blob) {
            return blob((Blob) result);
        } else if (result instanceof BlobList) {
            return blobs((BlobList) result);
        } else if (result instanceof DocumentRef) {
            CoreSession session = SessionFactory.getSession(request);
            return session.getDocument((DocumentRef) result);
        } else if ((result instanceof DocumentModel)
                || (result instanceof DocumentModelList)
                || (result instanceof JsonAdapter)) {
            return result;
        } else if (result instanceof RecordSet) {
            return result;
        } else if (result instanceof Paginable<?>) {
            return result;
        } else { // try to adapt to JSON
            return new DefaultJsonAdapter(result);
        }
    }

}
