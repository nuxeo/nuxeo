/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.jaxrs;

import java.security.Principal;
import java.util.HashSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.server.jaxrs.debug.DebugResource;
import org.nuxeo.ecm.automation.server.jaxrs.doc.DocResource;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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
     * Gets the content of the blob or blobs (multipart/mixed) located by the
     * given doc uid and property path.
     */
    @SuppressWarnings("unchecked")
    @GET
    @Path("files/{uid}")
    public Object getFile(@Context
    HttpServletRequest request, @PathParam("uid")
    String uid, @QueryParam("path")
    String path) {
        try {
            CoreSession session = SessionFactory.getSession(request);
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
                List<?> list = (List<?>) obj;
                if (list.isEmpty()) {
                    return ResponseHelper.notFound();
                }
                if (list.get(0) instanceof Blob) { // a list of blobs -> use
                    // multipart/mixed
                    return ResponseHelper.blobs((List<Blob>) list);
                }
            } else if (obj instanceof Blob) {
                return ResponseHelper.blob((Blob) obj);
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

    @POST
    @Path("login")
    public Object login(@Context
    HttpServletRequest request) {
        Principal p = request.getUserPrincipal();
        if (p instanceof NuxeoPrincipal) {
            NuxeoPrincipal np = (NuxeoPrincipal) p;
            List<String> groups = np.getAllGroups();
            HashSet<String> set = new HashSet<String>(groups);
            return new LoginInfo(np.getName(), set, np.isAdministrator());
        } else {
            return Response.status(401).build();
        }
    }

    @Path("{oid}")
    public Object getExecutable(@PathParam("oid")
    String oid) {
        if (oid.startsWith("Chain.")) {
            oid = oid.substring(6);
            return new ChainResource(service, oid);
        } else {
            try {
                OperationType op = service.getOperation(oid);
                return new OperationResource(service, op);
            } catch (Throwable e) {
                throw ExceptionHandler.newException(
                        "Failed to invoke operation: " + oid, e);
            }
        }
    }

}
