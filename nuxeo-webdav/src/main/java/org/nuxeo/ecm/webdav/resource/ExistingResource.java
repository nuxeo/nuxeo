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
import static javax.ws.rs.core.Response.Status.FORBIDDEN;

import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import net.java.dev.webdav.jaxrs.methods.COPY;
import net.java.dev.webdav.jaxrs.methods.LOCK;
import net.java.dev.webdav.jaxrs.methods.MKCOL;
import net.java.dev.webdav.jaxrs.methods.MOVE;
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
import net.java.dev.webdav.jaxrs.xml.properties.LockDiscovery;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.webdav.backend.Backend;
import org.nuxeo.ecm.webdav.backend.BackendHelper;
import org.nuxeo.ecm.webdav.jaxrs.Win32CreationTime;
import org.nuxeo.ecm.webdav.jaxrs.Win32FileAttributes;
import org.nuxeo.ecm.webdav.jaxrs.Win32LastAccessTime;
import org.nuxeo.ecm.webdav.jaxrs.Win32LastModifiedTime;

/**
 * An existing resource corresponds to an existing object (folder or file) in the repository.
 */
public class ExistingResource extends AbstractResource {

    public static final String READONLY_TOKEN = "readonly";

    public static final String DC_SOURCE = "dc:source";

    public static final String DC_CREATED = "dc:created";

    public static final Duration RECENTLY_CREATED_DELTA = Duration.ofMinutes(1);

    private static final Log log = LogFactory.getLog(ExistingResource.class);

    protected DocumentModel doc;

    protected Backend backend;

    protected ExistingResource(String path, DocumentModel doc, HttpServletRequest request, Backend backend) {
        super(path, request);
        this.doc = doc;
        this.backend = backend;
    }

    @DELETE
    public Response delete() {
        if (backend.isLocked(doc.getRef()) && !backend.canUnlock(doc.getRef())) {
            return Response.status(423).build();
        }

        // MS Office does the following to do a save on file.docx:
        // 1. save to tmp1.tmp
        // 2. rename file.docx to tmp2.tmp (here we saved the original name file.docx as "move original name")
        // 3. rename tmp1.tmp to file.docx
        // 4. remove tmp2.tmp (we're here, and the following code will undo the above logic)
        String origName;
        if (isMoveTargetCandidate(name) && (origName = getMoveOriginalName()) != null && !origName.contains("/")) {
            PathRef origRef = new PathRef(doc.getPath().removeLastSegments(1).append(origName).toString());
            CoreSession session = backend.getSession();
            if (session.exists(origRef)) {
                DocumentModel origDoc = session.getDocument(origRef);
                if (isRecentlyCreated(origDoc)) {
                    // origDoc is file.docx and contains the blob that was saved
                    // Move it to a temporary document that will be the one deleted at the end
                    String tmpName = UUID.randomUUID().toString() + ".tmp";
                    origDoc = backend.moveItem(origDoc, origDoc.getParentRef(), tmpName);
                    // Restore tmp2.tmp back to its original name file.docx
                    doc = backend.moveItem(doc, doc.getParentRef(), origName);
                    clearMoveOriginalName();
                    // Get the blob that was saved and update the restored doc file.docx with it
                    BlobHolder bh = origDoc.getAdapter(BlobHolder.class);
                    Blob blob = bh.getBlob();
                    blob.setFilename(origName);
                    doc.getAdapter(BlobHolder.class).setBlob(blob);
                    session.saveDocument(doc);
                    // Set the temporary document as current doc, which we can now delete
                    doc = origDoc;
                }
            }
        }

        try {
            backend.removeItem(doc.getRef());
            backend.saveChanges();
            return Response.status(204).build();
        } catch (DocumentSecurityException e) {
            log.error("Can't remove item: " + doc.getPathAsString() + e.getMessage());
            log.debug(e);
            return Response.status(FORBIDDEN).build();
        }
    }

    @COPY
    public Response copy(@HeaderParam("Destination") String dest, @HeaderParam("Overwrite") String overwrite) {
        return copyOrMove("COPY", dest, overwrite);
    }

    @MOVE
    public Response move(@HeaderParam("Destination") String dest, @HeaderParam("Overwrite") String overwrite) {
        if (backend.isLocked(doc.getRef()) && !backend.canUnlock(doc.getRef())) {
            return Response.status(423).build();
        }

        return copyOrMove("MOVE", dest, overwrite);
    }

    private static String encode(byte[] bytes, String encoding) {
        try {
            return new String(bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new NuxeoException("Unsupported encoding " + encoding);
        }
    }

    private Response copyOrMove(String method, @HeaderParam("Destination") String destination,
            @HeaderParam("Overwrite") String overwrite) {

        if (backend.isLocked(doc.getRef()) && !backend.canUnlock(doc.getRef())) {
            return Response.status(423).build();
        }

        destination = encode(destination.getBytes(), "ISO-8859-1");
        try {
            destination = URIUtil.decode(destination);
        } catch (URIException e) {
            throw new NuxeoException(e);
        }

        Backend root = BackendHelper.getBackend("/", request);
        Set<String> names = new HashSet<String>(root.getVirtualFolderNames());
        Path destinationPath = new Path(destination);
        String[] segments = destinationPath.segments();
        int removeSegments = 0;
        for (String segment : segments) {
            if (names.contains(segment)) {
                break;
            } else {
                removeSegments++;
            }
        }
        destinationPath = destinationPath.removeFirstSegments(removeSegments);

        String destPath = destinationPath.toString();
        String davDestPath = destPath;
        Backend destinationBackend = BackendHelper.getBackend(davDestPath, request);
        destPath = destinationBackend.parseLocation(destPath).toString();
        log.debug("to " + davDestPath);

        // Remove dest if it exists and the Overwrite header is set to "T".
        int status = 201;
        if (destinationBackend.exists(davDestPath)) {
            if ("F".equals(overwrite)) {
                return Response.status(412).build();
            }
            destinationBackend.removeItem(davDestPath);
            backend.saveChanges();
            status = 204;
        }

        // Check if parent exists
        String destParentPath = getParentPath(destPath);
        PathRef destParentRef = new PathRef(destParentPath);
        if (!destinationBackend.exists(getParentPath(davDestPath))) {
            return Response.status(409).build();
        }

        if ("COPY".equals(method)) {
            backend.copyItem(doc, destParentRef);
        } else if ("MOVE".equals(method)) {
            if (isMoveTargetCandidate(destPath)) {
                // MS Office tmp extension, the move may have to be undone later, so save the original name
                saveMoveOriginalName();
            }
            backend.moveItem(doc, destParentRef, getNameFromPath(destPath));
        }

        backend.saveChanges();
        return Response.status(status).build();
    }

    // Properties

    @PROPPATCH
    public Response proppatch(@Context UriInfo uriInfo) {
        if (backend.isLocked(doc.getRef()) && !backend.canUnlock(doc.getRef())) {
            return Response.status(423).build();
        }

        /*
         * JAXBContext jc = Util.getJaxbContext(); Unmarshaller u = jc.createUnmarshaller(); PropertyUpdate
         * propertyUpdate; try { propertyUpdate = (PropertyUpdate) u.unmarshal(request.getInputStream()); } catch
         * (JAXBException e) { return Response.status(400).build(); }
         */
        // Util.printAsXml(propertyUpdate);
        /*
         * List<RemoveOrSet> list = propertyUpdate.list(); final List<PropStat> propStats = new ArrayList<PropStat>();
         * for (RemoveOrSet set : list) { Prop prop = set.getProp(); List<Object> properties = prop.getProperties(); for
         * (Object property : properties) { PropStat propStat = new PropStat(new Prop(property), new Status(OK));
         * propStats.add(propStat); } }
         */

        // @TODO: patch properties if need.
        // Fake proppatch response
        @SuppressWarnings("deprecation")
        final net.java.dev.webdav.jaxrs.xml.elements.Response response = new net.java.dev.webdav.jaxrs.xml.elements.Response(
                new HRef(uriInfo.getRequestUri()), null, null, null, new PropStat(new Prop(new Win32CreationTime()),
                        new Status(OK)), new PropStat(new Prop(new Win32FileAttributes()), new Status(OK)),
                new PropStat(new Prop(new Win32LastAccessTime()), new Status(OK)), new PropStat(new Prop(
                        new Win32LastModifiedTime()), new Status(OK)));

        return Response.status(207).entity(new MultiStatus(response)).build();
    }

    /**
     * We can't MKCOL over an existing resource.
     */
    @MKCOL
    public Response mkcol() {
        return Response.status(405).build();
    }

    @HEAD
    public Response head() {
        return Response.status(200).build();
    }

    @LOCK
    public Response lock(@Context UriInfo uriInfo) {
        String token = null;
        Prop prop = null;
        if (backend.isLocked(doc.getRef())) {
            if (!backend.canUnlock(doc.getRef())) {
                return Response.status(423).build();
            } else {
                token = backend.getCheckoutUser(doc.getRef());
                prop = new Prop(getLockDiscovery(doc, uriInfo));
                return Response.ok().entity(prop).header("Lock-Token", "urn:uuid:" + token).build();
            }
        }

        token = backend.lock(doc.getRef());
        if (READONLY_TOKEN.equals(token)) {
            return Response.status(423).build();
        } else if (StringUtils.isEmpty(token)) {
            return Response.status(400).build();
        }

        prop = new Prop(getLockDiscovery(doc, uriInfo));

        backend.saveChanges();
        return Response.ok().entity(prop).header("Lock-Token", "urn:uuid:" + token).build();
    }

    @UNLOCK
    public Response unlock() {
        if (backend.isLocked(doc.getRef())) {
            if (!backend.canUnlock(doc.getRef())) {
                return Response.status(423).build();
            } else {
                backend.unlock(doc.getRef());
                backend.saveChanges();
                return Response.status(204).build();
            }
        } else {
            // TODO: return an error
            return Response.status(204).build();
        }
    }

    protected LockDiscovery getLockDiscovery(DocumentModel doc, UriInfo uriInfo) {
        LockDiscovery lockDiscovery = null;
        if (doc.isLocked()) {
            String token = backend.getCheckoutUser(doc.getRef());
            lockDiscovery = new LockDiscovery(new ActiveLock(LockScope.EXCLUSIVE, LockType.WRITE, Depth.ZERO,
                    new Owner(token), new TimeOut(10000L), new LockToken(new HRef("urn:uuid:" + token)), new LockRoot(
                            new HRef(uriInfo.getRequestUri()))));
        }
        return lockDiscovery;
    }

    protected PropStatBuilderExt getPropStatBuilderExt(DocumentModel doc, UriInfo uriInfo) throws
            URIException {
        Date lastModified = getTimePropertyWrapper(doc, "dc:modified");
        Date creationDate = getTimePropertyWrapper(doc, "dc:created");
        String displayName = URIUtil.encodePath(backend.getDisplayName(doc));
        PropStatBuilderExt props = new PropStatBuilderExt();
        props.lastModified(lastModified).creationDate(creationDate).displayName(displayName).status(OK);
        if (doc.isFolder()) {
            props.isCollection();
        } else {
            String mimeType = "application/octet-stream";
            long size = 0;
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            if (bh != null) {
                Blob blob = bh.getBlob();
                if (blob != null) {
                    size = blob.getLength();
                    mimeType = blob.getMimeType();
                }
            }
            if (StringUtils.isEmpty(mimeType) || "???".equals(mimeType)) {
                mimeType = "application/octet-stream";
            }
            props.isResource(size, mimeType);
        }
        return props;
    }

    protected Date getTimePropertyWrapper(DocumentModel doc, String name) {
        Object property;
        try {
            property = doc.getPropertyValue(name);
        } catch (PropertyNotFoundException e) {
            property = null;
            log.debug("Can't get property " + name + " from document " + doc.getId());
        }

        if (property != null) {
            return ((Calendar) property).getTime();
        } else {
            return new Date();
        }
    }

    protected boolean isMoveTargetCandidate(String path) {
        return path.endsWith(".tmp");
    }

    protected void saveMoveOriginalName() {
        doc.setPropertyValue(DC_SOURCE, name);
        doc = backend.getSession().saveDocument(doc);
    }

    protected String getMoveOriginalName() {
        return (String) doc.getPropertyValue(DC_SOURCE);
    }

    protected void clearMoveOriginalName() {
        doc.setPropertyValue(DC_SOURCE, null);
    }

    protected boolean isRecentlyCreated(DocumentModel doc) {
        Calendar created = (Calendar) doc.getPropertyValue(DC_CREATED);
        return created != null && created.toInstant().isAfter(Instant.now().minus(RECENTLY_CREATED_DELTA));
    }

}
