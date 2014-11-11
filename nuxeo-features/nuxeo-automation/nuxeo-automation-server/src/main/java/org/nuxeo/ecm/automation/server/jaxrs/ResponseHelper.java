/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.jaxrs;

import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.automation.server.jaxrs.io.MultipartBlobs;
import org.nuxeo.ecm.core.api.Blob;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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
        if (type == null) {
            type = MediaType.APPLICATION_OCTET_STREAM;
        }
        return Response.ok(blob).type(type).header("Content-Disposition",
                "attachment; filename=" + blob.getFilename()).build();
    }

    public static Response blobs(List<Blob> blobs) throws Exception {
        return new MultipartBlobs(blobs).getResponse();
    }

}
