/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webdav.resource;

import static javax.ws.rs.core.Response.Status.OK;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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

import net.java.dev.webdav.jaxrs.methods.PROPFIND;
import net.java.dev.webdav.jaxrs.xml.elements.HRef;
import net.java.dev.webdav.jaxrs.xml.elements.MultiStatus;
import net.java.dev.webdav.jaxrs.xml.elements.Prop;
import net.java.dev.webdav.jaxrs.xml.elements.PropFind;
import net.java.dev.webdav.jaxrs.xml.elements.PropStat;
import net.java.dev.webdav.jaxrs.xml.elements.Status;
import net.java.dev.webdav.jaxrs.xml.properties.LockDiscovery;
import net.java.dev.webdav.jaxrs.xml.properties.SupportedLock;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webdav.EscapeUtils;
import org.nuxeo.ecm.webdav.backend.Backend;
import org.nuxeo.ecm.webdav.jaxrs.IsFolder;
import org.nuxeo.ecm.webdav.jaxrs.Util;

/**
 * A resource for folder-like objects in the repository.
 */
public class FolderResource extends ExistingResource {

    private static final Log log = LogFactory.getLog(FolderResource.class);

    public FolderResource(String path, DocumentModel doc, HttpServletRequest request, Backend backend) {
        super(path, doc, request, backend);
    }

    @GET
    @Produces("text/html")
    public String get() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><p>");
        sb.append("Folder listing for ");
        sb.append(path);
        sb.append("/");
        sb.append("</p>\n<ul>\n");
        List<DocumentModel> children = backend.getChildren(doc.getRef());
        for (DocumentModel child : children) {
            String name = backend.getDisplayName(child);
            String qname = StringEscapeUtils.escapeHtml(name);
            sb.append("<li><a href=\"");
            sb.append(qname);
            if (child.isFolder()) {
                sb.append("/");
            }
            sb.append("\">");
            sb.append(qname);
            sb.append("</a></li>\n");
        }
        sb.append("</ul></body>\n");
        return sb.toString();
    }

    @PROPFIND
    @Produces({ "application/xml", "text/xml" })
    public Response propfind(@Context UriInfo uriInfo, @HeaderParam("depth") String depth)
            throws IOException, JAXBException {

        if (depth == null) {
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

        List<net.java.dev.webdav.jaxrs.xml.elements.Response> responses = new ArrayList<net.java.dev.webdav.jaxrs.xml.elements.Response>();
        responses.add(response);

        List<DocumentModel> children = backend.getChildren(doc.getRef());
        for (DocumentModel child : children) {
            net.java.dev.webdav.jaxrs.xml.elements.Response childResponse;
            childResponse = createResponse(child, uriInfo, prop);

            responses.add(childResponse);
        }

        MultiStatus st = new MultiStatus(
                responses.toArray(new net.java.dev.webdav.jaxrs.xml.elements.Response[responses.size()]));
        // printXml(st);
        return Response.status(207).entity(st).build();
    }

    protected net.java.dev.webdav.jaxrs.xml.elements.Response createResponse(DocumentModel doc, UriInfo uriInfo,
            Prop prop) {
        return createResponse(doc, uriInfo, prop, true);
    }

    protected net.java.dev.webdav.jaxrs.xml.elements.Response createResponse(DocumentModel doc, UriInfo uriInfo,
            Prop prop, boolean append) {
        PropStatBuilderExt props = getPropStatBuilderExt(doc, uriInfo);
        PropStat propStatFound = props.build();
        PropStat propStatNotFound = null;
        if (prop != null) {
            propStatNotFound = props.notFound(prop);
        }

        net.java.dev.webdav.jaxrs.xml.elements.Response response;
        UriBuilder uriBuilder = uriInfo.getRequestUriBuilder();
        if (append) {
            String path = EscapeUtils.encodePath(backend.getDisplayName(doc));
            uriBuilder.path(path);
        }
        URI uri = uriBuilder.build();
        if (doc.isFolder()) {
            PropStat folderPropStat = new PropStat(
                    new Prop(new LockDiscovery(), new SupportedLock(), new IsFolder("t")), new Status(OK));
            if (propStatNotFound != null) {
                response = new net.java.dev.webdav.jaxrs.xml.elements.Response(new HRef(uri), null, null, null,
                        propStatFound, propStatNotFound, folderPropStat);
            } else {
                response = new net.java.dev.webdav.jaxrs.xml.elements.Response(new HRef(uri), null, null, null,
                        propStatFound, folderPropStat);
            }
        } else {
            if (propStatNotFound != null) {
                response = new net.java.dev.webdav.jaxrs.xml.elements.Response(new HRef(uri), null, null, null,
                        propStatFound, propStatNotFound);
            } else {
                response = new net.java.dev.webdav.jaxrs.xml.elements.Response(new HRef(uri), null, null, null,
                        propStatFound);
            }
        }
        return response;
    }

}
