/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *     vpasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.server.jaxrs;

import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.List;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationNotFoundException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.jaxrs.LoginInfo;
import org.nuxeo.ecm.automation.jaxrs.io.operations.AutomationInfo;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Path("/automation")
@WebObject(type = "automation")
public class AutomationResource extends ModuleRoot {

    protected AutomationService service;

    public AutomationResource() {
        service = Framework.getService(AutomationService.class);
    }

    @Path("/doc")
    public Object getDocPage() {
        return newObject("doc");
    }

    @Path("/debug")
    public Object getDebugPage() {
        return newObject("debug");
    }

    /**
     * Gets the content of the blob or blobs (multipart/mixed) located by the given doc uid and property path.
     */
    @SuppressWarnings("unchecked")
    @GET
    @Path("/files/{uid}")
    public Object getFile(@Context HttpServletRequest request, @PathParam("uid") String uid,
            @QueryParam("path") String path) {
        try {
            CoreSession session = SessionFactory.getSession(request);
            DocumentModel doc = session.getDocument(new IdRef(uid));
            Object obj;
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
                return obj; // BlobWriter will do all the processing and call the DownloadService
            }
            return ResponseHelper.notFound();
        } catch (MessagingException | IOException e) {
            throw new NuxeoException(e);
        }
    }

    @GET
    public AutomationInfo doGet() throws OperationException {
        return new AutomationInfo(service);
    }

    @POST
    @Path("/login")
    public Object login(@Context HttpServletRequest request) {
        Principal p = request.getUserPrincipal();
        if (p instanceof NuxeoPrincipal) {
            NuxeoPrincipal np = (NuxeoPrincipal) p;
            List<String> groups = np.getAllGroups();
            HashSet<String> set = new HashSet<>(groups);
            return new LoginInfo(np.getName(), set, np.isAdministrator());
        } else {
            return Response.status(401).build();
        }
    }

    @Path("/{oid}")
    public Object getExecutable(@PathParam("oid") String oid) {
        if (oid.startsWith(Constants.CHAIN_ID_PREFIX)) {
            oid = oid.substring(6);
        }
        try {
            OperationType op = service.getOperation(oid);
            return newObject("operation", op);
        } catch (OperationNotFoundException cause) {
            return new WebResourceNotFoundException("Failed to invoke operation: " + oid, cause);
        }
    }

}
