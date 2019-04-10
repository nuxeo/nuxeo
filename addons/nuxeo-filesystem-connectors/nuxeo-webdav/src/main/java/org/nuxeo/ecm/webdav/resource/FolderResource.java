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
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webdav.Util;
import org.nuxeo.ecm.webdav.backend.WebDavBackend;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
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

        // Get key properties from doc
        Date lastModified = getTimePropertyWrapper(doc, "dc:modified");
        Date creationDate = getTimePropertyWrapper(doc, "dc:created");

        final net.java.dev.webdav.jaxrs.xml.elements.Response response
                = new net.java.dev.webdav.jaxrs.xml.elements.Response(
                new HRef(uriInfo.getRequestUri()),
                null,
                null,
                null,
                new PropStat(
                        new Prop(
                                new DisplayName("nuxeo"),
                                new LockDiscovery(),
                                new SupportedLock(),
                                new IsFolder("t"),
                                new IsCollection(1),
                                new IsHidden(0),
                                new GetContentType("application/octet-stream"),
                                new GetContentLength(0),
                                new CreationDate(creationDate),
                                new GetLastModified(lastModified),
                                COLLECTION
                        ),
                        new Status(OK)));

        if (!doc.isFolder() || depth.equals("0")) {
            return Response.status(207).entity(new MultiStatus(response)).build();
        }

        List<net.java.dev.webdav.jaxrs.xml.elements.Response> responses
                = new ArrayList<net.java.dev.webdav.jaxrs.xml.elements.Response>();
        responses.add(response);

        List<DocumentModel> children = backend.getChildren(doc.getRef());
        for (DocumentModel child : children) {
            lastModified = getTimePropertyWrapper(child, "dc:modified");
            creationDate = getTimePropertyWrapper(child, "dc:created");
            String childName = URIUtil.encodePath(backend.getDisplayName(child));
            PropStatBuilderExt props = new PropStatBuilderExt();
            props.lastModified(lastModified).creationDate(creationDate).displayName(childName).status(OK);
            if (child.isFolder()) {
                props.isCollection();
            } else {
                Blob blob = (Blob) child.getPropertyValue("file:content");
                String mimeType = "application/octet-stream";
                long size = 0;
                if (blob != null) {
                    size = blob.getLength();
                    mimeType = blob.getMimeType();
                }
                if(StringUtils.isEmpty(mimeType) || "???".equals(mimeType) ){
                    mimeType = "application/octet-stream";
                }
                props.isResource(size, mimeType);
            }

            PropStat found = props.build();
            PropStat notFound = null;
            if (prop != null) {
                // props.isHidden(false);
                // props.lastAccessed(lastModified);
                notFound = props.notFound(prop);
            }

            net.java.dev.webdav.jaxrs.xml.elements.Response childResponse;
            URI childUri = uriInfo.getRequestUriBuilder().path(childName).build();
            if (notFound != null) {
                childResponse = new net.java.dev.webdav.jaxrs.xml.elements.Response(
                        new HRef(childUri), null, null, null, found, notFound);
            } else {
                childResponse = new net.java.dev.webdav.jaxrs.xml.elements.Response(
                        new HRef(childUri), null, null, null, found);
            }

            responses.add(childResponse);
        }

        MultiStatus st = new MultiStatus(responses.toArray(
                new net.java.dev.webdav.jaxrs.xml.elements.Response[responses.size()]));
        //printXml(st);
        return Response.status(207).entity(st).build();
    }

}
