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

import net.java.dev.webdav.jaxrs.methods.*;
import net.java.dev.webdav.jaxrs.xml.elements.*;
import net.java.dev.webdav.jaxrs.xml.properties.CreationDate;
import net.java.dev.webdav.jaxrs.xml.properties.GetLastModified;
import net.java.dev.webdav.jaxrs.xml.properties.ResourceType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webdav.Constants;
import org.nuxeo.ecm.webdav.resource.PropStatBuilderExt;
import org.nuxeo.ecm.webdav.Util;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static javax.ws.rs.core.Response.Status.OK;
import static net.java.dev.webdav.jaxrs.xml.properties.ResourceType.COLLECTION;

/**
 * An existing resource corresponds to an existing document (folder or file)
 * in the repository.
 */
public class ExistingResource extends AbstractResource {

    private static final Log log = LogFactory.getLog(ExistingResource.class);

    protected DocumentModel doc;

    protected ExistingResource(String path, DocumentModel doc) throws Exception {
        super(path);
        this.doc = doc;
    }

    @DELETE
    public Response delete() throws Exception {
        DocumentRef ref = new PathRef(path);
        session.removeDocument(ref);
        session.save();
        return Response.ok().build();
    }

    @COPY
    public Response copy(@HeaderParam("Destination") String dest,
            @HeaderParam("Overwrite") String overwrite) throws Exception {
        return copyOrMove("COPY", dest, overwrite);
    }

    @MOVE
    public Response move(@HeaderParam("Destination") String dest,
            @HeaderParam("Overwrite") String overwrite) throws Exception {
        return copyOrMove("MOVE", dest, overwrite);
    }

    private Response copyOrMove(String method, @HeaderParam("Destination") String dest,
            @HeaderParam("Overwrite") String overwrite) throws Exception {
        URI destUri = new URI(dest);
        String destPath = destUri.getPath();
        while (destPath.endsWith("/")) {
            destPath = destPath.substring(0, destPath.length()-1);
        }
        destPath = destPath.substring(Constants.DAV_HOME.length(), destPath.length());
        log.info("to " + destPath);

        DocumentRef sourceRef = new PathRef(path);
        DocumentRef destRef = new PathRef(destPath);

        String destParentPath = getParentPath(destPath);
        PathRef destParentRef = new PathRef(destParentPath);
        if (!session.exists(destParentRef)) {
            return Response.status(409).build();
        }

        // Remove dest if it exists and the Overwrite header is set to "T".
        int status = 201;
        if (session.exists(destRef)) {
            if ("F".equals(overwrite)) {
                return Response.status(412).build();
            }
            session.removeDocument(destRef);
            status = 204;
        }

        session.copy(sourceRef, destParentRef, getName(destPath));
        if ("MOVE".equals(method)) {
            session.removeDocument(sourceRef);
        }
        session.save();

        return Response.status(status).entity("").build();
    }

    // Properties

    @PROPFIND
    @Consumes("*/*")
    @Produces("application/xml")
    public Response propfind(@Context UriInfo uriInfo, @Context HttpServletRequest request,
            @HeaderParam("depth") String depth) throws Exception {
        log.info("Depth=" + depth);
        JAXBContext jc = Util.getJaxbContext();
        Unmarshaller u = jc.createUnmarshaller();

        PropFind propFind;
        try {
            propFind = (PropFind) u.unmarshal(request.getInputStream());
        } catch (JAXBException e) {
            log.error(e);
            // FIXME: check this is the right response code
            return Response.status(400).build();
        }
        Prop prop = propFind.getProp();

        // Get key properties from doc
        Date lastModified = ((Calendar) doc.getPropertyValue("dc:modified")).getTime();
        Date creationDate = ((Calendar) doc.getPropertyValue("dc:created")).getTime();

        final net.java.dev.webdav.jaxrs.xml.elements.Response response
                = new net.java.dev.webdav.jaxrs.xml.elements.Response(
                        new HRef(uriInfo.getRequestUri()), null, null, null,
                        new PropStat(
                                new Prop(new CreationDate(creationDate), new GetLastModified(lastModified), COLLECTION),
                                new Status(OK)));

        if (!doc.isFolder() || depth.equals("0")) {
            return Response.status(207).entity(new MultiStatus(response)).build();
        }

        List<net.java.dev.webdav.jaxrs.xml.elements.Response> responses
                = new ArrayList<net.java.dev.webdav.jaxrs.xml.elements.Response>();
        responses.add(response);

        List<DocumentModel> children = session.getChildren(doc.getRef());
        for (DocumentModel child : children) {
            lastModified = ((Calendar) child.getPropertyValue("dc:modified")).getTime();
            creationDate = ((Calendar) child.getPropertyValue("dc:created")).getTime();
            String childName = child.getName();
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
                props.isResource(size, mimeType);
            }

            PropStat found = props.build();
            PropStat notFound = null;
            if (prop != null) {
                // props.isHidden(false);
                // props.lastAccessed(lastModified);
                notFound = props.notFound(prop);
            }

            net.java.dev.webdav.jaxrs.xml.elements.Response davFile;
            if (notFound != null) {
                davFile = new net.java.dev.webdav.jaxrs.xml.elements.Response(
                        new HRef(uriInfo.getRequestUriBuilder().path(childName).build()),
                        null, null, null, found, notFound);
            } else {
                davFile = new net.java.dev.webdav.jaxrs.xml.elements.Response(
                        new HRef(uriInfo.getRequestUriBuilder().path(childName).build()),
                        null, null, null, found);
            }

            responses.add(davFile);
        }

        MultiStatus st = new MultiStatus(responses.toArray(
                        new net.java.dev.webdav.jaxrs.xml.elements.Response[responses.size()]));
        printXml(st);
        return Response.status(207).entity(st).build();
    }

    @PROPPATCH
    public Response proppatch(@Context UriInfo uriInfo, @Context HttpServletRequest request) throws Exception {
        JAXBContext jc = Util.getJaxbContext();
        Unmarshaller u = jc.createUnmarshaller();
        PropertyUpdate propertyUpdate;
        try {
            propertyUpdate = (PropertyUpdate) u.unmarshal(request.getInputStream());
        } catch (JAXBException e) {
            return Response.status(400).build();
        }
        printXml(propertyUpdate);
        return Response.ok().build();
    }

    /**
     * We can't MKCOL over an existing resource.
     */
    @MKCOL
    public Response mkcol() {
        return Response.status(405).build();
    }

    // Unimplemented for now

    @LOCK
    public Response lock() {
        return Response.status(500).build();
    }

    @UNLOCK
    public Response unlock() {
        return Response.status(500).build();
    }

    // For debugging.

    private static void printXml(Object o) throws JAXBException {
        StringWriter sw = new StringWriter();
        Marshaller marshaller = Util.getJaxbContext().createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(o, sw);
        System.out.println(sw);
    }

}
