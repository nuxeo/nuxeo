package org.nuxeo.ecm.webdav.resource;

import net.java.dev.webdav.core.jaxrs.xml.properties.IsCollection;
import net.java.dev.webdav.core.jaxrs.xml.properties.IsFolder;
import net.java.dev.webdav.core.jaxrs.xml.properties.IsHidden;
import net.java.dev.webdav.jaxrs.methods.*;
import net.java.dev.webdav.jaxrs.xml.elements.*;
import net.java.dev.webdav.jaxrs.xml.properties.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.*;

import static javax.ws.rs.core.Response.Status.OK;
import static net.java.dev.webdav.jaxrs.xml.properties.ResourceType.COLLECTION;

/**
 * @author Organization: Gagnavarslan ehf
 */
public class VirtualFolderResource extends AbstractResource {

    private static final Log log = LogFactory.getLog(VirtualFolderResource.class);

    private LinkedList<String> rootFolderNames;

    public VirtualFolderResource(String path, HttpServletRequest request, LinkedList<String> rootFolderNames) throws Exception {
        super(path, request);
        this.rootFolderNames = rootFolderNames;
    }

    @GET
    @Produces("text/html")
    public String get() throws ClientException {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><p>Folder listing for " + path + ":</p>\n<ul>");
        for (String name : rootFolderNames) {
            sb.append("<li><a href='" + name + "'>" + name + "</a></li>\n");
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

        Date lastModified = new Date();
        Date creationDate = new Date();

        final net.java.dev.webdav.jaxrs.xml.elements.Response response
                = new net.java.dev.webdav.jaxrs.xml.elements.Response(
                new HRef(uriInfo.getRequestUri()),
                null,
                null,
                null,
                new PropStat(
                        new Prop(
                                new DisplayName("nuxeo"), /* @TODO: fix this. Hardcoded root name*/
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

        if (depth.equals("0")) {
            return Response.status(207).entity(new MultiStatus(response)).build();
        }

        List<net.java.dev.webdav.jaxrs.xml.elements.Response> responses
                = new ArrayList<net.java.dev.webdav.jaxrs.xml.elements.Response>();
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

            childResponse = new net.java.dev.webdav.jaxrs.xml.elements.Response(
                        new HRef(childUri), null, null, null, found);

            responses.add(childResponse);
        }

        MultiStatus st = new MultiStatus(responses.toArray(
                new net.java.dev.webdav.jaxrs.xml.elements.Response[responses.size()]));
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
        Prop prop = new Prop(new LockDiscovery(new ActiveLock(
                LockScope.EXCLUSIVE, LockType.WRITE, Depth.ZERO,
                new Owner("Administrator"),
                new TimeOut(10000L), new LockToken(new HRef("urn:uuid:Administrator")),
                new LockRoot(new HRef(uriInfo.getRequestUri()))
        )));

        return Response.ok().entity(prop)
                .header("Lock-Token", "urn:uuid:Administrator").build();
    }

    @UNLOCK
    public Response unlock() throws Exception {
        return Response.status(204).build();
    }


}
