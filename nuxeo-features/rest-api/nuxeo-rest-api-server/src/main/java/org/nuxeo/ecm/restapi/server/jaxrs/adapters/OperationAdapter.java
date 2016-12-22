/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.server.jaxrs.adapters;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.impl.InvokableMethod;
import org.nuxeo.ecm.automation.core.impl.ChainTypeImpl;
import org.nuxeo.ecm.automation.jaxrs.io.operations.ExecutionRequest;
import org.nuxeo.ecm.automation.server.AutomationServer;
import org.nuxeo.ecm.automation.server.jaxrs.ResponseHelper;
import org.nuxeo.ecm.automation.server.jaxrs.RestOperationException;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.ExceptionHelper;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7.2 - Web adapter that expose how to run an operation on a document
 */
@WebAdapter(name = OperationAdapter.NAME, type = "OperationService")
@Produces({ "application/json+nxentity", "application/json+esentity", MediaType.APPLICATION_JSON })
public class OperationAdapter extends DefaultAdapter {

    public static final String NAME = "op";

    @POST
    @Path("{operationName}")
    public Response doPost(@PathParam("operationName") String oid, @Context HttpServletRequest request,
            @Context HttpServletResponse response, ExecutionRequest xreq) {
        try {
            AutomationServer srv = Framework.getLocalService(AutomationServer.class);
            if (!srv.accept(oid, false, request)) {
                return ResponseHelper.notFound();
            }

            AutomationService service = Framework.getLocalService(AutomationService.class);

            OperationType operationType = service.getOperation(oid);

            // If chain, taking the first operation to do the input lookup after
            if (operationType instanceof ChainTypeImpl) {
                OperationChain chain = ((ChainTypeImpl) operationType).getChain();
                if (!chain.getOperations().isEmpty()) {
                    operationType = service.getOperation(chain.getOperations().get(0).id());
                } else {
                    throw new WebException("Chain '" + oid + "' doesn't contain any operation");
                }
            }

            for (InvokableMethod method : operationType.getMethods()) {
                if (getTarget().getAdapter(method.getInputType()) != null) {
                    xreq.setInput(getTarget().getAdapter(method.getInputType()));
                    break;
                }
            }

            OperationContext ctx = xreq.createContext(request, response, getContext().getCoreSession());
            Object result = service.run(ctx, oid, xreq.getParams());

            int customHttpStatus = xreq.getRestOperationContext().getHttpStatus();
            return Response.status(customHttpStatus).entity(result).build();
        } catch (OperationException cause) {
            if (ExceptionHelper.unwrapException(cause) instanceof RestOperationException) {
                int customHttpStatus = ((RestOperationException) ExceptionHelper.unwrapException(cause)).getStatus();
                throw WebException.newException("Failed to invoke operation: " + oid, cause, customHttpStatus);
            }
            throw WebException.newException("Failed to invoke operation: " + oid, cause);
        }

    }

}
