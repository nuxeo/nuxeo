/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.automation.server.jaxrs.debug;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.inject.Inject;
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
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.OperationChainContribution;
import org.nuxeo.ecm.automation.jaxrs.io.JsonWriter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webengine.model.Access;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebObject(type = "debug", administrator = Access.GRANT)
public class DebugResource extends AbstractResource<ResourceTypeImpl> {

    private static final Log log = LogFactory.getLog(DebugResource.class);

    static final XMap xmap = new XMap();
    static {
        xmap.register(OperationChainContribution.class);
    }

    @Inject
    AutomationService service;

    @Inject
    OperationContext ctx;

    @Inject
    CoreSession session;

    public AutomationService getOperationService() {
        return service;
    }

    public String getOperationsListAsJson() throws OperationException, IOException {
        return JsonWriter.exportOperations();
    }

    @GET
    @Produces("text/html")
    public Object doGet() {
        return getView("index");
    }

    @GET
    @Produces("text/plain")
    @Path("/doc")
    public Object doGetText() throws OperationException, IOException {
        return getOperationsListAsJson();
    }

    /**
     * @since 5.9.1
     */
    @GET
    @Produces("text/plain")
    @Path("/studioDoc")
    public Object doGetStudioDoc() throws OperationException, IOException {
        return JsonWriter.exportOperations(true);
    }

    @GET
    @Produces("application/json")
    public Object doGetJSON() throws OperationException, IOException {
        return getOperationsListAsJson();
    }

    @POST
    public Response doPost(@FormParam("input") String input, @FormParam("chain") String chainXml) {
        if (!session.getPrincipal().isAdministrator()) {
            return Response.status(403).build();
        }
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(chainXml.getBytes());
            OperationChainContribution contrib = (OperationChainContribution) xmap.load(in);
            OperationChain chain = contrib.toOperationChain(Framework.getRuntime().getContext().getBundle());
            ctx.setInput(getDocumentRef(input));
            getOperationService().run(ctx, chain);
            return Response.ok("Operation Done.").build();
        } catch (NuxeoException | OperationException | IOException e) {
            log.error(e, e);
            return Response.status(500).build();
        }
    }

    @POST
    @Path("{chainId}")
    public Response doChainIdPost(@FormParam("input") String input, @FormParam("chainId") String chainId) {
        try {
            ctx.setInput(getDocumentRef(input));
            getOperationService().run(ctx, chainId);
            return Response.ok("Operation Done.").build();
        } catch (OperationException e) {
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
