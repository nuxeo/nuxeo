/*
 * (C) Copyright 2006-2026 Nuxeo (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.webdav.resource;

import static jakarta.ws.rs.core.Response.Status.OK;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.JAXBException;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jugs.webdav.jaxrs.methods.PROPFIND;
import org.jugs.webdav.jaxrs.xml.elements.HRef;
import org.jugs.webdav.jaxrs.xml.elements.MultiStatus;
import org.jugs.webdav.jaxrs.xml.elements.Prop;
import org.jugs.webdav.jaxrs.xml.elements.PropFind;
import org.jugs.webdav.jaxrs.xml.elements.PropStat;
import org.jugs.webdav.jaxrs.xml.elements.Status;
import org.jugs.webdav.jaxrs.xml.properties.LockDiscovery;
import org.jugs.webdav.jaxrs.xml.properties.SupportedLock;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webdav.EscapeUtils;
import org.nuxeo.ecm.webdav.backend.Backend;
import org.nuxeo.ecm.webdav.jaxrs.IsFolder;
import org.nuxeo.ecm.webdav.jaxrs.Util;
import org.xml.sax.SAXException;

/**
 * A resource for folder-like objects in the repository.
 */
public class FolderResource extends ExistingResource {

    private static final Logger log = LogManager.getLogger(FolderResource.class);

    public FolderResource(String path, DocumentModel doc, HttpServletRequest request, Backend backend) {
        super(path, doc, request, backend);
    }

    @GET
    @Produces("text/html")
    public String get() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><p>");
        sb.append("Folder listing for ");
        sb.append(StringEscapeUtils.escapeHtml4(path));
        sb.append("/");
        sb.append("</p>\n<ul>\n");
        List<DocumentModel> children = backend.getChildren(doc.getRef());
        for (DocumentModel child : children) {
            String name = backend.getDisplayName(child);
            sb.append("<li><a href=\"");
            sb.append(StringEscapeUtils.escapeHtml4(EscapeUtils.encodePath(name)));
            if (child.isFolder()) {
                sb.append("/");
            }
            sb.append("\">");
            sb.append(StringEscapeUtils.escapeHtml4(name));
            sb.append("</a></li>\n");
        }
        sb.append("</ul></body>\n");
        return sb.toString();
    }

    @PROPFIND
    @Produces({ "application/xml", "text/xml" })
    public Response propfind(@Context UriInfo uriInfo, @HeaderParam("depth") String depth)
            throws IOException, JAXBException, ParserConfigurationException, SAXException {

        if (depth == null) {
            depth = "1";
        }

        Prop prop = null;
        if (request.getInputStream() != null && request.getContentLength() > 0) {
            PropFind propFind;
            try {
                propFind = (PropFind) Util.unmarshal(request.getInputStream());
            } catch (JAXBException e) {
                log.error(e);
                // FIXME: check this is the right response code
                return Response.status(400).build();
            }
            prop = propFind.getProp();
            // Util.printAsXml(prop);
        }

        final org.jugs.webdav.jaxrs.xml.elements.Response response;
        response = createResponse(doc, uriInfo, prop, false);

        if (!doc.isFolder() || depth.equals("0")) {
            return Response.status(207).entity(new MultiStatus(response)).build();
        }

        List<org.jugs.webdav.jaxrs.xml.elements.Response> responses = new ArrayList<>();
        responses.add(response);

        List<DocumentModel> children = backend.getChildren(doc.getRef());
        for (DocumentModel child : children) {
            org.jugs.webdav.jaxrs.xml.elements.Response childResponse;
            childResponse = createResponse(child, uriInfo, prop);

            responses.add(childResponse);
        }

        MultiStatus st = new MultiStatus(
                responses.toArray(new org.jugs.webdav.jaxrs.xml.elements.Response[responses.size()]));
        // printXml(st);
        return Response.status(207).entity(st).build();
    }

    protected org.jugs.webdav.jaxrs.xml.elements.Response createResponse(DocumentModel doc, UriInfo uriInfo,
            Prop prop) {
        return createResponse(doc, uriInfo, prop, true);
    }

    protected org.jugs.webdav.jaxrs.xml.elements.Response createResponse(DocumentModel doc, UriInfo uriInfo, Prop prop,
            boolean append) {
        PropStatBuilderExt props = getPropStatBuilderExt(doc, uriInfo);
        PropStat propStatFound = props.build();
        PropStat propStatNotFound = null;
        if (prop != null) {
            propStatNotFound = props.notFound(prop);
        }

        org.jugs.webdav.jaxrs.xml.elements.Response response;
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
                response = new org.jugs.webdav.jaxrs.xml.elements.Response(new HRef(uri), null, null, null,
                        propStatFound, propStatNotFound, folderPropStat);
            } else {
                response = new org.jugs.webdav.jaxrs.xml.elements.Response(new HRef(uri), null, null, null,
                        propStatFound, folderPropStat);
            }
        } else {
            if (propStatNotFound != null) {
                response = new org.jugs.webdav.jaxrs.xml.elements.Response(new HRef(uri), null, null, null,
                        propStatFound, propStatNotFound);
            } else {
                response = new org.jugs.webdav.jaxrs.xml.elements.Response(new HRef(uri), null, null, null,
                        propStatFound);
            }
        }
        return response;
    }

}
