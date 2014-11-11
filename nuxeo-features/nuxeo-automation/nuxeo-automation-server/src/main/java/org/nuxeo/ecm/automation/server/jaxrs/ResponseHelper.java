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
 */
package org.nuxeo.ecm.automation.server.jaxrs;

import java.util.List;

import javax.ws.rs.core.Response;

import org.nuxeo.ecm.automation.server.jaxrs.io.MultipartBlobs;
import org.nuxeo.ecm.core.api.Blob;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ResponseHelper {

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
        return Response.ok(blob).type(blob.getMimeType()).header(
                "Content-Disposition",
                "attachment; filename=" + blob.getFilename()).build();
    }

    public static Response blobs(List<Blob> blobs) throws Exception {
        return new MultipartBlobs(blobs).getResponse();
    }

}
