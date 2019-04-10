/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Gagnavarslan ehf
 */
package org.nuxeo.ecm.webdav.resource;

import static javax.ws.rs.core.Response.Status.OK;
import static net.java.dev.webdav.jaxrs.xml.properties.ResourceType.COLLECTION;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import net.java.dev.webdav.jaxrs.methods.COPY;
import net.java.dev.webdav.jaxrs.methods.LOCK;
import net.java.dev.webdav.jaxrs.methods.MKCOL;
import net.java.dev.webdav.jaxrs.methods.MOVE;
import net.java.dev.webdav.jaxrs.methods.PROPFIND;
import net.java.dev.webdav.jaxrs.methods.PROPPATCH;
import net.java.dev.webdav.jaxrs.methods.UNLOCK;
import net.java.dev.webdav.jaxrs.xml.elements.ActiveLock;
import net.java.dev.webdav.jaxrs.xml.elements.Depth;
import net.java.dev.webdav.jaxrs.xml.elements.HRef;
import net.java.dev.webdav.jaxrs.xml.elements.LockRoot;
import net.java.dev.webdav.jaxrs.xml.elements.LockScope;
import net.java.dev.webdav.jaxrs.xml.elements.LockToken;
import net.java.dev.webdav.jaxrs.xml.elements.LockType;
import net.java.dev.webdav.jaxrs.xml.elements.MultiStatus;
import net.java.dev.webdav.jaxrs.xml.elements.Owner;
import net.java.dev.webdav.jaxrs.xml.elements.Prop;
import net.java.dev.webdav.jaxrs.xml.elements.PropStat;
import net.java.dev.webdav.jaxrs.xml.elements.Status;
import net.java.dev.webdav.jaxrs.xml.elements.TimeOut;
import net.java.dev.webdav.jaxrs.xml.properties.CreationDate;
import net.java.dev.webdav.jaxrs.xml.properties.DisplayName;
import net.java.dev.webdav.jaxrs.xml.properties.GetContentLength;
import net.java.dev.webdav.jaxrs.xml.properties.GetContentType;
import net.java.dev.webdav.jaxrs.xml.properties.GetLastModified;
import net.java.dev.webdav.jaxrs.xml.properties.LockDiscovery;
import net.java.dev.webdav.jaxrs.xml.properties.SupportedLock;

import org.apache.commons.lang.StringEscapeUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.webdav.jaxrs.IsCollection;
import org.nuxeo.ecm.webdav.jaxrs.IsFolder;
import org.nuxeo.ecm.webdav.jaxrs.IsHidden;

public class VirtualFolderResource extends AbstractResource {

    private LinkedList<String> rootFolderNames;

    public VirtualFolderResource(String path, HttpServletRequest request, LinkedList<String> rootFolderNames)
            throws Exception {
        super(path, request);
        this.rootFolderNames = rootFolderNames;
    }

    @GET
    @Produces("text/html")
    public String get() throws ClientException {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><p>");
        sb.append("Folder listing for ");
        sb.append(path);
        sb.append("/");
        sb.append("</p>\n<ul>\n");
        for (String name : rootFolderNames) {
            String qname = StringEscapeUtils.escapeHtml(name);
            sb.append("<li><a href=\"");
            sb.append(qname);
            sb.append("/"); // terminating slash
            sb.append("\">");
            sb.append(qname);
            sb.append("</a></li>\n");
        }
        sb.append("</ul></body>\n");
        return sb.toString();
    }

    @PROPFIND
    public Response propfind(@Context UriInfo uriInfo, @HeaderParam("depth") String depth) throws Exception {

        if (depth == null) {
            depth = "1";
        }

        Date lastModified = new Date();
        Date creationDate = new Date();

        @SuppressWarnings("deprecation")
        final net.java.dev.webdav.jaxrs.xml.elements.Response response = new net.java.dev.webdav.jaxrs.xml.elements.Response(
                new HRef(uriInfo.getRequestUri()), null, null, null, new PropStat(new Prop(new DisplayName("nuxeo"), /*
                                                                                                                      * @
                                                                                                                      * TODO
                                                                                                                      * :
                                                                                                                      * fix
                                                                                                                      * this
                                                                                                                      * .
                                                                                                                      * Hardcoded
                                                                                                                      * root
                                                                                                                      * name
                                                                                                                      */
                new LockDiscovery(), new SupportedLock(), new IsFolder("t"), new IsCollection(Integer.valueOf(1)),
                        new IsHidden(Integer.valueOf(0)), new GetContentType("application/octet-stream"),
                        new GetContentLength(0), new CreationDate(creationDate), new GetLastModified(lastModified),
                        COLLECTION), new Status(OK)));

        if (depth.equals("0")) {
            return Response.status(207).entity(new MultiStatus(response)).build();
        }

        List<net.java.dev.webdav.jaxrs.xml.elements.Response> responses = new ArrayList<net.java.dev.webdav.jaxrs.xml.elements.Response>();
        responses.add(response);

        for (String name : rootFolderNames) {
            lastModified = new Date();
            creationDate = new Date();
            PropStatBuilderExt props = new PropStatBuilderExt();
            props.lastModified(lastModified).creationDate(creationDate).displayName(name).status(OK);
            props.isCollection();

            PropStat found = props.build();

            net.java.dev.webdav.jaxrs.xml.elements.Response childResponse;
            URI childUri = uriInfo.getRequestUriBuilder().path(name).build();

            childResponse = new net.java.dev.webdav.jaxrs.xml.elements.Response(new HRef(childUri), null, null, null,
                    found);

            responses.add(childResponse);
        }

        MultiStatus st = new MultiStatus(
                responses.toArray(new net.java.dev.webdav.jaxrs.xml.elements.Response[responses.size()]));
        return Response.status(207).entity(st).build();
    }

    @DELETE
    public Response delete() throws Exception {
        return Response.status(401).build();
    }

    @COPY
    public Response copy() throws Exception {
        return Response.status(401).build();
    }

    @MOVE
    public Response move() throws Exception {
        return Response.status(401).build();
    }

    @PROPPATCH
    public Response proppatch() throws Exception {
        return Response.status(401).build();
    }

    @MKCOL
    public Response mkcol() {
        return Response.status(405).build();
    }

    @HEAD
    public Response head() {
        return Response.status(404).build();
    }

    @LOCK
    public Response lock(@Context UriInfo uriInfo) throws Exception {
        Prop prop = new Prop(new LockDiscovery(new ActiveLock(LockScope.EXCLUSIVE, LockType.WRITE, Depth.ZERO,
                new Owner("Administrator"), new TimeOut(10000L), new LockToken(new HRef("urn:uuid:Administrator")),
                new LockRoot(new HRef(uriInfo.getRequestUri())))));

        return Response.ok().entity(prop).header("Lock-Token", "urn:uuid:Administrator").build();
    }

    @UNLOCK
    public Response unlock() throws Exception {
        return Response.status(204).build();
    }

}
