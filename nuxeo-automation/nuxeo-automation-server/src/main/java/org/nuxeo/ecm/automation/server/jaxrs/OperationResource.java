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

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.doc.JSONExporter;
import org.nuxeo.ecm.automation.core.doc.bonita.BonitaExporter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class OperationResource extends ExecutableResource {

    protected OperationType type;

    public OperationResource(AutomationService service, OperationType type) {
        super(service);
        this.type = type;
    }

    @GET
    @Produces("application/json")
    public Object doGet() {
        try {
            OperationDocumentation doc = type.getDocumentation();
            JSONObject json = JSONExporter.toJSON(doc);
            return Response.ok(json.toString(2)).type("application/json").build();
        } catch (IOException e) {
            return Response.status(500).build();
        }
    }

    @GET
    @Produces("application/zip")
    @Path("bonita")
    public Object doGetBonitaZip() {
        try {
            OperationDocumentation doc = type.getDocumentation();
            InputStream res = BonitaExporter.getZip(doc);
            return Response.ok(res).type("application/zip").build();
        } catch (IOException e) {
            return Response.status(500).build();
        }
    }

    @GET
    @Produces("text/plain")
    @Path("bonita/java")
    public Object doGetBonitaJava() {
        try {
            OperationDocumentation doc = type.getDocumentation();
            String java = BonitaExporter.getJavaClass(doc);
            return Response.ok(java).type("text/plain").build();
        } catch (IOException e) {
            return Response.status(500).build();
        }
    }

    @GET
    @Produces("text/plain")
    @Path("bonita/xml")
    public Object doGetBonitaXML() {
        try {
            OperationDocumentation doc = type.getDocumentation();
            String java = BonitaExporter.getXMLDescription(doc);
            return Response.ok(java).type("text/plain").build();
        } catch (IOException e) {
            return Response.status(500).build();
        }
    }

    @Override
    public Object execute(ExecutionRequest xreq) throws Exception {
        OperationContext ctx = xreq.createContext(request, getCoreSession());
        return service.run(ctx, xreq.createChain(type));
    }

    @Override
    public String getId() {
        return type.getId();
    }

    @Override
    public boolean isChain() {
        return false;
    }
}
