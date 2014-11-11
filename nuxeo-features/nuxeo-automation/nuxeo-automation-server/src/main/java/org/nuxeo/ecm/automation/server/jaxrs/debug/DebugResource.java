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
package org.nuxeo.ecm.automation.server.jaxrs.debug;

import java.io.ByteArrayInputStream;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.OperationChainContribution;
import org.nuxeo.ecm.automation.core.doc.JSONExporter;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.view.TemplateView;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DebugResource {

    static XMap xmap = new XMap();
    static {
        xmap.register(OperationChainContribution.class);
    }


    public AutomationService getOperationService() throws Exception {
        return Framework.getLocalService(AutomationService.class);
    }

    public String getOperationsListAsJson() throws Exception {
        return JSONExporter.toJSON();
    }

    //TODO HTML page
    @GET
    @Produces("text/html")
    public Object doGet() throws Exception {
        return new TemplateView(this, "index.ftl.html");
    }

    @GET
    @Produces("text/plain")
    @Path("doc")
    public Object doGetText() throws Exception {
        return JSONExporter.toJSON();
    }

    @GET
    @Produces("application/json")
    public Object doGetJSON() throws Exception {
        return JSONExporter.toJSON();
    }

    @POST
    public Response doPost(@FormParam("input") String input,
            @FormParam("chain") String chainXml) throws Exception {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(chainXml.getBytes());
            OperationChainContribution contrib = (OperationChainContribution)xmap.load(in);
            OperationChain chain = contrib.toOperationChain(Framework.getRuntime().getContext().getBundle());
            OperationContext ctx = new OperationContext(WebEngine.getActiveContext().getCoreSession());
            ctx.setInput(getDocumentRef(input));
            getOperationService().run(ctx, chain);
            return Response.ok("Operation Done.").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500).build();
        }
    }

    @POST
    @Path("{chainId}")
    public Response doChainIdPost(@FormParam("input") String input,
            @FormParam("chainId") String chainId) throws Exception {
        try {
            OperationContext ctx = new OperationContext(WebEngine.getActiveContext().getCoreSession());
            ctx.setInput(getDocumentRef(input));
            getOperationService().run(ctx, chainId);
            return Response.ok("Operation Done.").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500).build();
        }
    }

    protected DocumentRef getDocumentRef(String ref) {
        if (ref == null || ref.length() == 0) {
            return null;
        }
        if (ref.startsWith("/")) {
            return new PathRef(ref);
        } else {
            return new IdRef(ref);
        }
    }

}
