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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.server.jaxrs.debug.DebugResource;
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

    @Path("debug")
    public Object getDebugPage() {
        return new DebugResource();
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
