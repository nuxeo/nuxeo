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

import net.java.dev.webdav.core.jaxrs.xml.properties.IsCollection;
import net.java.dev.webdav.core.jaxrs.xml.properties.IsFolder;
import net.java.dev.webdav.core.jaxrs.xml.properties.IsHidden;
import net.java.dev.webdav.jaxrs.methods.PROPFIND;
import net.java.dev.webdav.jaxrs.xml.elements.*;
import net.java.dev.webdav.jaxrs.xml.properties.*;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.webdav.Util;
import org.nuxeo.ecm.webdav.backend.WebDavBackend;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static javax.ws.rs.core.Response.Status.OK;
import static net.java.dev.webdav.jaxrs.xml.properties.ResourceType.COLLECTION;

/**
 * A resource for folder-like objects in the repository.
 */
public class FolderResource extends ExistingResource {

    private static final Log log = LogFactory.getLog(FolderResource.class);

    public FolderResource(String path, DocumentModel doc, HttpServletRequest request, WebDavBackend backend) throws Exception {
        super(path, doc, request, backend);
    }

    @GET
    @Produces("text/html")
    public String get() throws ClientException {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><p>Folder listing for " + path + ":</p>\n<ul>");
        List<DocumentModel> children = backend.getChildren(doc.getRef());
        for (DocumentModel child : children) {
            String childName = backend.getDisplayName(child);
            // TODO: properly escape.
            sb.append("<li><a href='" + childName + "'>" + childName + "</a></li>\n");
        }
        sb.append("</ul></body>\n");

        return sb.toString();
    }

    @PROPFIND
    public Response propfind(@Context UriInfo uriInfo,
            @HeaderParam("depth") String depth
            ) throws Exception {

        if(depth == null){
            depth = "1";
        }

        Unmarshaller u = Util.getUnmarshaller();

        Prop prop = null;
        if (request.getInputStream() != null && request.getContentLength() > 0) {
            PropFind propFind;
            try {
                propFind = (PropFind) u.unmarshal(request.getInputStream());
            } catch (JAXBException e) {
                log.error(e);
                // FIXME: check this is the right response code
                return Response.status(400).build();
            }
            prop = propFind.getProp();
            // Util.printAsXml(prop);
        }

        final net.java.dev.webdav.jaxrs.xml.elements.Response response;
        response = createResponse(doc, uriInfo, prop, false);

        if (!doc.isFolder() || depth.equals("0")) {
            return Response.status(207).entity(new MultiStatus(response)).build();
        }

        List<net.java.dev.webdav.jaxrs.xml.elements.Response> responses
                = new ArrayList<net.java.dev.webdav.jaxrs.xml.elements.Response>();
        responses.add(response);

        List<DocumentModel> children = backend.getChildren(doc.getRef());
        for (DocumentModel child : children) {
            net.java.dev.webdav.jaxrs.xml.elements.Response childResponse;
            childResponse = createResponse(child, uriInfo, prop);

            responses.add(childResponse);
        }

        MultiStatus st = new MultiStatus(responses.toArray(
                new net.java.dev.webdav.jaxrs.xml.elements.Response[responses.size()]));
        //printXml(st);
        return Response.status(207).entity(st).build();
    }

    protected net.java.dev.webdav.jaxrs.xml.elements.Response createResponse(
            DocumentModel doc, UriInfo uriInfo, Prop prop)
                    throws ClientException, URIException {
        return createResponse(doc, uriInfo, prop, true);
    }

    protected net.java.dev.webdav.jaxrs.xml.elements.Response createResponse(
            DocumentModel doc, UriInfo uriInfo, Prop prop, boolean append)
            throws ClientException, URIException {
        PropStatBuilderExt props = getPropStatBuilderExt(doc, uriInfo);
        PropStat propStatFound = props.build();
        PropStat propStatNotFound = null;
        if (prop != null) {
            propStatNotFound = props.notFound(prop);
        }

        net.java.dev.webdav.jaxrs.xml.elements.Response response;
        UriBuilder uriBuilder = uriInfo.getRequestUriBuilder();
        if (append) {
            uriBuilder.path(URIUtil.encodePath(backend.getDisplayName(doc)));
        }
        URI uri = uriBuilder.build();
        if (doc.isFolder()) {
            PropStat folderPropStat = new PropStat(
                    new Prop(new LockDiscovery(), new SupportedLock(), new IsFolder("t")),
                    new Status(OK));
            if (propStatNotFound != null) {
                response = new net.java.dev.webdav.jaxrs.xml.elements.Response(
                        new HRef(uri), null, null, null,
                        propStatFound, propStatNotFound, folderPropStat);
            } else {
                response = new net.java.dev.webdav.jaxrs.xml.elements.Response(
                        new HRef(uri), null, null, null,
                        propStatFound, folderPropStat);
            }
        } else {
            if (propStatNotFound != null) {
                response = new net.java.dev.webdav.jaxrs.xml.elements.Response(
                        new HRef(uri), null, null, null, propStatFound, propStatNotFound);
            } else {
                response = new net.java.dev.webdav.jaxrs.xml.elements.Response(
                        new HRef(uri), null, null, null, propStatFound);
            }
        }
        return response;
    }

}
