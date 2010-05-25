/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.jaxrs;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.server.jaxrs.debug.DebugResource;
import org.nuxeo.ecm.automation.server.jaxrs.doc.DocResource;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.webengine.session.UserSession;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Path("automation")
public class AutomationResource {

    protected AutomationService service;

    public AutomationResource() throws Exception {
        service = Framework.getService(AutomationService.class);
    }


    @Path("doc")
    public Object getDocPage() {
        return new DocResource();
    }

    @Path("debug")
    public Object getDebugPage() {
        return new DebugResource();
    }

    /**
     * Get the content of the blob or blobs (multipart/mixed) located by the given doc uid and property path.
     * @param uid
     * @param path
     * @return
     */
    @SuppressWarnings("unchecked")
    @GET
    @Path("files/{uid}")
    public Object getFile(@Context HttpServletRequest request, @PathParam("uid") String uid, @QueryParam("path") String path) {
        try {
            CoreSession session = UserSession.getCurrentSession(request).getCoreSession();
            DocumentModel doc = session.getDocument(new IdRef(uid));
            Object obj = null;
            try {
                obj = doc.getPropertyValue(path);
            } catch (PropertyException e) {
                return ResponseHelper.notFound();
            }
            if (obj == null) {
                return ResponseHelper.notFound();
            }
            if (obj instanceof List<?>) {
                List<?> list = (List<?>)obj;
                if (list.isEmpty()) {
                    return ResponseHelper.notFound();
                }
                if (list.get(0) instanceof Blob) { // a list of blobs -> use multipart/mixed
                    return ResponseHelper.blobs((List<Blob>)list);
                }
            } else if (obj instanceof Blob) {
                return ResponseHelper.blob((Blob)obj);
            }
            return ResponseHelper.notFound();
        } catch (Exception e) {
            throw ExceptionHandler.newException(e);
        }
    }

    @GET
    public AutomationInfo doGet() {
        return new AutomationInfo(service);
    }

    @Path("{oid}")
    public Object getExecutable(@PathParam("oid") String oid) {
        if (oid.startsWith("Chain.")) {
            oid = oid.substring(6);
            return new ChainResource(service, oid);
        } else {
            try {
                OperationType op = service.getOperation(oid);
                return new OperationResource(service, op);
            } catch (Throwable e) {
                throw ExceptionHandler.newException("Failed to invoke operation: "+oid, e);
            }
        }
    }

}
