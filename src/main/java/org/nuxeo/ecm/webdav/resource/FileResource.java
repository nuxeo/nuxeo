/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webdav.resource;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.runtime.services.streaming.InputStreamSource;

public class FileResource extends ExistingResource {

    public FileResource(String path, DocumentModel doc) throws Exception {
        super(path, doc);
    }

    @GET
    public Response get() throws Exception {
        Blob content = (Blob) doc.getPropertyValue("file:content");
        if (content == null) {
            return Response.ok("").build();
        } else {
            return Response.ok(content.getStream()).type(content.getMimeType()).build();
        }
    }

    @PUT
    public Response put(@Context HttpServletRequest request) throws Exception {
        Blob content = new StreamingBlob(new InputStreamSource(request.getInputStream()));
        content.setMimeType(request.getContentType());
        content.setFilename(name);

        DocumentModel newdoc = session.createDocumentModel(parentPath, name, "File");

        newdoc.getProperty("file:content").setValue(content);
        session.createDocument(newdoc);
        session.save();

        return Response.created(new URI(path)).build();
    }

}
