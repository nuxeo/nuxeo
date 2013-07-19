/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.automation.rest.jaxrs.adapters;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationNotFoundException;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.server.AutomationServer;
import org.nuxeo.ecm.automation.server.jaxrs.ExceptionHandler;
import org.nuxeo.ecm.automation.server.jaxrs.ExecutionRequest;
import org.nuxeo.ecm.automation.server.jaxrs.ResponseHelper;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.runtime.api.Framework;

/**
 * Web adapter that expose how to run an operation on a document
 *
 * @since 5.7.2
 */
@WebAdapter(name = OperationAdapter.NAME, type = "OperationService")
@Produces({ "application/json+nxentity", MediaType.APPLICATION_JSON })
public class OperationAdapter extends DefaultAdapter {

    public static final String NAME = "op";

    @POST
    @Path("{operationName}")
    public Response doPost(@PathParam("operationName")
    String oid, @Context
    HttpServletRequest request, ExecutionRequest xreq) {
        try {
            AutomationServer srv = Framework.getLocalService(AutomationServer.class);
            if (!srv.accept(oid, isChain(oid), request)) {
                return ResponseHelper.notFound();
            }

            AutomationService service = Framework.getLocalService(AutomationService.class);

            DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
            if (doc != null) {
                xreq.setInput(doc);
            } else {
                DocumentModelList docs = getTarget().getAdapter(
                        DocumentModelList.class);
                xreq.setInput(docs);
            }

            OperationContext ctx = xreq.createContext(request,
                    getContext().getCoreSession());

            ctx.putAll(xreq.getParams());

            OperationChain chain = getOperationChain(service, oid,
                    xreq.getParams());

            return Response.ok(service.run(ctx, chain)).build();
        } catch (Throwable e) {
            throw ExceptionHandler.newException("Failed to execute operation: "
                    + oid, e);
        }

    }

    private boolean isChain(String oid) {
        return oid.startsWith("Chain.");
    }

    private OperationChain getOperationChain(AutomationService service,
            String oid, Map<String, Object> params)
            throws OperationNotFoundException {

        if (isChain(oid)) {
            return service.getOperationChain(getRealChainId(oid));
        } else {
            OperationChain chain = new OperationChain("operation");
            OperationParameters oparams = new OperationParameters(oid, params);
            chain.add(oparams);
            return chain;
        }
    }

    private String getRealChainId(String oid) {
        return isChain(oid) ? oid.substring(6) : oid;
    }
}
