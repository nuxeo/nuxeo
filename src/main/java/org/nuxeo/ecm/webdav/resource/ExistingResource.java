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

import static javax.ws.rs.core.Response.Status.OK;
import static net.java.dev.webdav.jaxrs.xml.properties.ResourceType.COLLECTION;

import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import net.java.dev.webdav.jaxrs.methods.COPY;

import net.java.dev.webdav.jaxrs.methods.LOCK;
import net.java.dev.webdav.jaxrs.methods.MKCOL;
import net.java.dev.webdav.jaxrs.methods.MOVE;
import net.java.dev.webdav.jaxrs.methods.PROPFIND;
import net.java.dev.webdav.jaxrs.methods.PROPPATCH;
import net.java.dev.webdav.jaxrs.methods.UNLOCK;
import net.java.dev.webdav.jaxrs.xml.conditions.CannotModifyProtectedProperty;
import net.java.dev.webdav.jaxrs.xml.conditions.LockTokenMatchesRequestUri;
import net.java.dev.webdav.jaxrs.xml.conditions.LockTokenSubmitted;
import net.java.dev.webdav.jaxrs.xml.conditions.NoConflictingLock;
import net.java.dev.webdav.jaxrs.xml.conditions.NoExternalEntities;
import net.java.dev.webdav.jaxrs.xml.conditions.PreservedLiveProperties;
import net.java.dev.webdav.jaxrs.xml.conditions.PropFindFiniteDepth;
import net.java.dev.webdav.jaxrs.xml.elements.ActiveLock;
import net.java.dev.webdav.jaxrs.xml.elements.AllProp;
import net.java.dev.webdav.jaxrs.xml.elements.Collection;
import net.java.dev.webdav.jaxrs.xml.elements.Depth;
import net.java.dev.webdav.jaxrs.xml.elements.Error;
import net.java.dev.webdav.jaxrs.xml.elements.Exclusive;
import net.java.dev.webdav.jaxrs.xml.elements.HRef;
import net.java.dev.webdav.jaxrs.xml.elements.Include;
import net.java.dev.webdav.jaxrs.xml.elements.Location;
import net.java.dev.webdav.jaxrs.xml.elements.LockEntry;
import net.java.dev.webdav.jaxrs.xml.elements.LockInfo;
import net.java.dev.webdav.jaxrs.xml.elements.LockRoot;
import net.java.dev.webdav.jaxrs.xml.elements.LockScope;
import net.java.dev.webdav.jaxrs.xml.elements.LockToken;
import net.java.dev.webdav.jaxrs.xml.elements.LockType;
import net.java.dev.webdav.jaxrs.xml.elements.MultiStatus;
import net.java.dev.webdav.jaxrs.xml.elements.Owner;
import net.java.dev.webdav.jaxrs.xml.elements.Prop;
import net.java.dev.webdav.jaxrs.xml.elements.PropFind;
import net.java.dev.webdav.jaxrs.xml.elements.PropName;
import net.java.dev.webdav.jaxrs.xml.elements.PropStat;
import net.java.dev.webdav.jaxrs.xml.elements.PropertyUpdate;
import net.java.dev.webdav.jaxrs.xml.elements.Remove;
import net.java.dev.webdav.jaxrs.xml.elements.ResponseDescription;
import net.java.dev.webdav.jaxrs.xml.elements.Set;
import net.java.dev.webdav.jaxrs.xml.elements.Shared;
import net.java.dev.webdav.jaxrs.xml.elements.Status;
import net.java.dev.webdav.jaxrs.xml.elements.TimeOut;
import net.java.dev.webdav.jaxrs.xml.elements.Write;
import net.java.dev.webdav.jaxrs.xml.properties.CreationDate;
import net.java.dev.webdav.jaxrs.xml.properties.DisplayName;
import net.java.dev.webdav.jaxrs.xml.properties.GetContentLanguage;
import net.java.dev.webdav.jaxrs.xml.properties.GetContentLength;
import net.java.dev.webdav.jaxrs.xml.properties.GetContentType;
import net.java.dev.webdav.jaxrs.xml.properties.GetETag;
import net.java.dev.webdav.jaxrs.xml.properties.GetLastModified;
import net.java.dev.webdav.jaxrs.xml.properties.LockDiscovery;
import net.java.dev.webdav.jaxrs.xml.properties.ResourceType;
import net.java.dev.webdav.jaxrs.xml.properties.SupportedLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webdav.Constants;
import org.nuxeo.ecm.webdav.PropStatBuilderExt;

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
    public Response propfind(@Context UriInfo uriInfo, @Context HttpServletRequest request,
            @HeaderParam("depth") int depth) throws Exception {
        log.info("Depth=" + depth);
        JAXBContext jc = getJaxbContext();
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

        Date lastModified = ((Calendar) doc.getPropertyValue("dc:modified")).getTime();
        Date creationDate = ((Calendar) doc.getPropertyValue("dc:created")).getTime();

        final net.java.dev.webdav.jaxrs.xml.elements.Response response
                = new net.java.dev.webdav.jaxrs.xml.elements.Response(
                        new HRef(uriInfo.getRequestUri()), null, null, null,
                        new PropStat(
                                new Prop(new CreationDate(creationDate), new GetLastModified(lastModified), COLLECTION),
                                new Status(OK)));

        if (!doc.isFolder() || depth == 0) {
            //printXml(new MultiStatus(response));
            return Response.ok(new MultiStatus(response)).build();
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
        //printXml(st);
        return Response.ok(st).build();
    }

    @PROPPATCH
    public Response proppatch(@Context UriInfo uriInfo, @Context HttpServletRequest request) throws Exception {
        JAXBContext jc = getJaxbContext();
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

    // utility methods related to JAXB marshalling

    private static JAXBContext getJaxbContext() throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(new Class<?>[] {
                // Minimal set
                // PropFind.class, PropertyUpdate.class,

                // Full set
                ActiveLock.class, AllProp.class, CannotModifyProtectedProperty.class, Collection.class,
                CreationDate.class, Depth.class, DisplayName.class, Error.class, Exclusive.class,
                GetContentLanguage.class, GetContentLength.class, GetContentType.class, GetETag.class,
                GetLastModified.class, HRef.class, Include.class, Location.class, LockDiscovery.class, LockEntry.class,
                LockInfo.class, LockRoot.class, LockScope.class, LockToken.class, LockTokenMatchesRequestUri.class,
                LockTokenSubmitted.class, LockType.class, MultiStatus.class, NoConflictingLock.class,
                NoExternalEntities.class, Owner.class, PreservedLiveProperties.class, Prop.class, PropertyUpdate.class,
                PropFind.class, PropFindFiniteDepth.class, PropName.class, PropStat.class, Remove.class,
                ResourceType.class, Response.class, ResponseDescription.class, Set.class, Shared.class, Status.class,
                SupportedLock.class, TimeOut.class, Write.class});
        return jc;
    }

    // For debug.

    private static void printXml(Object o) throws JAXBException {
        StringWriter sw = new StringWriter();
        Marshaller marshaller = getJaxbContext().createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(o, sw);
        System.out.println(sw);
    }

}
