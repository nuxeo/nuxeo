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
import java.util.Calendar;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static javax.ws.rs.core.Response.Status.OK;
import net.java.dev.webdav.jaxrs.methods.PROPFIND;
import net.java.dev.webdav.jaxrs.xml.elements.*;
import net.java.dev.webdav.jaxrs.xml.properties.CreationDate;
import net.java.dev.webdav.jaxrs.xml.properties.GetContentLength;
import net.java.dev.webdav.jaxrs.xml.properties.GetContentType;
import net.java.dev.webdav.jaxrs.xml.properties.GetLastModified;
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

        doc.getProperty("file:content").setValue(content);
        session.saveDocument(doc);
        session.save();

        return Response.created(new URI(path)).build();
    }

    @PROPFIND
	public Response propfind(@Context UriInfo uriInfo) throws Exception {

        Date lastModified = ((Calendar) doc.getPropertyValue("dc:modified")).getTime();
        Date creationDate = ((Calendar) doc.getPropertyValue("dc:created")).getTime();

		net.java.dev.webdav.jaxrs.xml.elements.Response response;
        response = new net.java.dev.webdav.jaxrs.xml.elements.Response(
                new HRef(uriInfo.getRequestUri()),
                null,
                null,
                null,
                new PropStat(new Prop(
                        new CreationDate(lastModified), new GetLastModified(lastModified),
                        new GetContentType("application/octet-stream"), new GetContentLength(0 /* FIXME */)),
                        new Status(OK)));

		MultiStatus st = new MultiStatus(response);
		return Response.ok(st).build();
	}

}
