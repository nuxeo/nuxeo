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
package org.nuxeo.ecm.automation.server.jaxrs.debug;

import java.io.ByteArrayInputStream;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.OperationChainContribution;
import org.nuxeo.ecm.automation.core.doc.JSONExporter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.ecm.webengine.jaxrs.views.TemplateView;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DebugResource {

    private static final Log log = LogFactory.getLog(DebugResource.class);

    static final XMap xmap = new XMap();
    static {
        xmap.register(OperationChainContribution.class);
    }

    public AutomationService getOperationService() {
        return Framework.getLocalService(AutomationService.class);
    }

    public String getOperationsListAsJson() throws Exception {
        return JSONExporter.toJSON();
    }

    @GET
    @Produces("text/html")
    public Object doGet() {
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
            @FormParam("chain") String chainXml) {
        CoreSession session = SessionFactory.getSession();
        if (!((NuxeoPrincipal) session.getPrincipal()).isAdministrator()) {
            return Response.status(403).build();
        }
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(
                    chainXml.getBytes());
            OperationChainContribution contrib = (OperationChainContribution) xmap.load(in);
            OperationChain chain = contrib.toOperationChain(Framework.getRuntime().getContext().getBundle());
            OperationContext ctx = new OperationContext(session);
            ctx.setInput(getDocumentRef(input));
            getOperationService().run(ctx, chain);
            return Response.ok("Operation Done.").build();
        } catch (Exception e) {
            log.error(e, e);
            return Response.status(500).build();
        }
    }

    @POST
    @Path("{chainId}")
    public Response doChainIdPost(@FormParam("input") String input,
            @FormParam("chainId") String chainId) {
        try {
            OperationContext ctx = new OperationContext(
                    SessionFactory.getSession());
            ctx.setInput(getDocumentRef(input));
            getOperationService().run(ctx, chainId);
            return Response.ok("Operation Done.").build();
        } catch (Exception e) {
            log.error(e, e);
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
