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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.core.Context;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.server.AutomationServer;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.session.UserSession;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public abstract class ExecutableResource {

    @Context
    protected HttpServletRequest request;

    protected AutomationService service;

    public ExecutableResource(AutomationService service) {
        this.service = service;
    }

    public CoreSession getCoreSession() {
        return UserSession.getCurrentSession(request).getCoreSession();
    }

    @POST
    public Object doPost(@Context HttpServletRequest request,
            ExecutionRequest xreq) {
        this.request = request;
        try {
            AutomationServer srv = Framework.getLocalService(AutomationServer.class);
            if (!srv.accept(getId(), isChain(), request)) {
                return ResponseHelper.notFound();
            }
            Object result = execute(xreq);
            if ("true".equals(request.getHeader("X-NXVoidOperation"))) {
                return ResponseHelper.emptyContent(); // void response
            }
            if (result instanceof Blob) {
                return ResponseHelper.blob((Blob) result);
            } else if (result instanceof BlobList) {
                return ResponseHelper.blobs((BlobList) result);
            } else {
                return result;
            }
        } catch (Throwable e) {
            throw ExceptionHandler.newException("Failed to execute operation: "
                    + getId(), e);
        }
    }

    public abstract String getId();

    public abstract Object execute(ExecutionRequest req) throws Exception;

    public abstract boolean isChain();

}
