/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.ecm.automation.server.jaxrs.adapters;

import java.io.Serializable;
import java.net.URI;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationCallback;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.impl.InvokableMethod;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.jaxrs.io.operations.ExecutionRequest;
import org.nuxeo.ecm.automation.server.AutomationServer;
import org.nuxeo.ecm.automation.server.jaxrs.OperationResource;
import org.nuxeo.ecm.automation.server.jaxrs.ResponseHelper;
import org.nuxeo.ecm.automation.server.jaxrs.RestOperationException;
import org.nuxeo.ecm.core.api.AsyncService;
import org.nuxeo.ecm.core.api.AsyncStatus;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.ExceptionHelper;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @since 10.3
 */
@WebAdapter(name = AsyncOperationAdapter.NAME, type = "AsyncOperationAdapter", targetType = "operation")
@Produces({ MediaType.APPLICATION_JSON })
public class AsyncOperationAdapter extends DefaultAdapter {

    public static final String NAME = "async";

    private static final Logger log = LogManager.getLogger(AsyncOperationAdapter.class);

    protected static final String STATUS_STORE_NAME = "automation";

    protected String TRANSIENT_STORE_SERVICE = "service";

    protected String TRANSIENT_STORE_TASK_ID = "taskId";

    protected String TRANSIENT_STORE_ERROR = "error";

    protected String TRANSIENT_STORE_OUTPUT = "output";

    protected String TRANSIENT_STORE_OUTPUT_BLOB = "blob";

    @Context
    protected AutomationService service;

    @Context
    protected HttpServletRequest request;

    @Context
    protected HttpServletResponse response;

    @Context
    protected CoreSession session;

    @POST
    public Object doPost(ExecutionRequest xreq) {
        OperationResource op = (OperationResource) getTarget();
        String opId = op.getId();

        try {
            AutomationServer srv = Framework.getService(AutomationServer.class);
            if (!srv.accept(opId, op.isChain(), request)) {
                return ResponseHelper.notFound();
            }
            final String executionId = UUID.randomUUID().toString();

            // session will be set in the task thread
            OperationContext opCtx = xreq.createContext(request, response, null);

            opCtx.setCallback(new OperationCallback() {

                @Override
                public void onChainEnter(OperationType chain) {
                    //
                }

                @Override
                public void onChainExit() {
                    setCompleted(executionId);
                }

                @Override
                public void onOperationEnter(OperationContext context, OperationType type, InvokableMethod method,
                        Map<String, Object> params) {
                    enterMethod(executionId, method);
                }

                @Override
                public void onOperationExit(Object output) {
                    setOutput(executionId, (Serializable) output);
                }

                @Override
                public OperationException onError(OperationException error) {
                    setError(executionId, error.getMessage());
                    return error;
                }

            });

            String repoName = session.getRepositoryName();
            NuxeoPrincipal principal = session.getPrincipal();

            // TODO NXP-26303: use thread pool
            new Thread(() -> {
                TransactionHelper.runInTransaction(() -> {
                    try (CloseableCoreSession session = CoreInstance.openCoreSession(repoName, principal)){
                        opCtx.setCoreSession(session);
                        service.run(opCtx, opId, xreq.getParams());
                    } catch (OperationException e) {
                        setError(executionId, e.getMessage());
                    }
                });
            }).start();

            String statusURL = String.format("%s/%s/status", ctx.getURL(), executionId);
            return Response.status(HttpServletResponse.SC_ACCEPTED).location(new URI(statusURL)).build();

        } catch (URISyntaxException cause) {
            String exceptionMessage = "Failed to invoke operation: " + op.getId();
            Throwable unWrapException = ExceptionHelper.unwrapException(cause);
            if (unWrapException instanceof RestOperationException) {
                int customHttpStatus = ((RestOperationException) unWrapException).getStatus();
                throw new NuxeoException(exceptionMessage, cause, customHttpStatus);
            }
            throw new NuxeoException(exceptionMessage, cause);
        }
    }

    @GET
    @Path("{executionId}/status")
    public Object status(@PathParam("executionId") String executionId)
            throws IOException, URISyntaxException, MessagingException {
        if (isCompleted(executionId)) {
            String error = getError(executionId);
            if (error != null) {
                throw new NuxeoException(error, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            String resURL = StringUtils.removeEnd(ctx.getURL(), "/status");
            return redirect(resURL);
        } else {
            Object result = "RUNNING";
            if (isAsync(executionId)) {
                Serializable taskId = getTaskId(executionId);
                result = getAsyncService(executionId).getStatus(taskId);
            }
            return ResponseHelper.getResponse(result, request, HttpServletResponse.SC_OK);
        }
    }

    @GET
    @Path("{executionId}")
    public Object result(@PathParam("executionId") String executionId) throws IOException, MessagingException {

        if (isCompleted(executionId)) {
            Object output = getResult(executionId);

            String error = getError(executionId);

            // cleanup after result is accessed
            cleanup(executionId);

            if (error != null) {
                throw new NuxeoException(error, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

            // if output has a "url" key assume it's a redirect url
            if (output instanceof Map) {
                Object url = ((Map) output).get("url");
                if (url instanceof String) {
                    String baseUrl = VirtualHostHelper.getBaseURL(ctx.getRequest());
                    return redirect(baseUrl + url);
                }
            }
            return ResponseHelper.getResponse(output, request, HttpServletResponse.SC_OK);
        }

        throw new WebResourceNotFoundException("Execution with id=" + executionId + " not found");
    }

    @DELETE
    @Path("{executionId}")
    public Object abort(@PathParam("executionId") String executionId) throws IOException, MessagingException {
        if (exists(executionId) && !isCompleted(executionId)) {
            // TODO NXP-26304: support aborting any execution
            if (isAsync(executionId)) {
                Serializable taskId = getTaskId(executionId);
                return getAsyncService(executionId).abort(taskId);
            }
            return ResponseHelper.getResponse("RUNNING", request, HttpServletResponse.SC_OK);
        }
        throw new WebResourceNotFoundException("Execution with id=" + executionId + " has completed");
    }

    protected TransientStore getTransientStore() {
        return Framework.getService(TransientStoreService.class).getStore(STATUS_STORE_NAME);
    }

    protected void enterMethod(String executionId, InvokableMethod method) {
        Map<String, Serializable> parameters = new HashMap<>();
        // AsyncService.class is default => not async
        if (!AsyncService.class.equals(method.getAsyncService())) {
            parameters.put(TRANSIENT_STORE_SERVICE, method.getAsyncService().getName());
        }
        // reset parameters
        getTransientStore().putParameters(executionId, parameters);
    }

    protected void setError(String executionId, String error) {
        getTransientStore().putParameter(executionId, TRANSIENT_STORE_ERROR, error);
        setCompleted(executionId);
    }

    public String getError(String executionId) {
        return (String) getTransientStore().getParameter(executionId, TRANSIENT_STORE_ERROR);
    }

    protected void setOutput(String executionId, Serializable output) {
        // store only taskId for async tasks
        if (isAsync(executionId)) {
            Serializable taskId = output instanceof AsyncStatus ? ((AsyncStatus) output).getId() : output;
            getTransientStore().putParameter(executionId, TRANSIENT_STORE_TASK_ID, taskId);
        } else {
            if (output instanceof DocumentModel) {
                output = detach((DocumentModel) output);
            } else if (output instanceof DocumentModelList) {
                output = new DocumentModelListImpl(
                        ((DocumentModelList) output).stream().map(this::detach).collect(Collectors.toList()));
            }
            if (output instanceof Blob) {
                getTransientStore().putParameter(executionId, TRANSIENT_STORE_OUTPUT_BLOB, true);
                getTransientStore().putBlobs(executionId, Collections.singletonList((Blob) output));
            } else if (output instanceof BlobList) {
                getTransientStore().putParameter(executionId, TRANSIENT_STORE_OUTPUT_BLOB, false);
                getTransientStore().putBlobs(executionId, (BlobList) output);
            } else {
                getTransientStore().putParameter(executionId, TRANSIENT_STORE_OUTPUT, output);
            }
        }
    }

    protected Object getResult(String executionId) {
        if (isAsync(executionId)) {
            AsyncService service = getAsyncService(executionId);
            if (service != null) {
                Serializable taskId = getTransientStore().getParameter(executionId, TRANSIENT_STORE_TASK_ID);
                return service.getResult(taskId);
            }
        }

        Object output;
        List<Blob> blobs = getTransientStore().getBlobs(executionId);
        if (CollectionUtils.isNotEmpty(blobs)) {
            boolean isSingle = (boolean) getTransientStore().getParameter(executionId, TRANSIENT_STORE_OUTPUT_BLOB);
            output = isSingle ? blobs.get(0) : new BlobList(blobs);
        } else {
            output = getTransientStore().getParameter(executionId, TRANSIENT_STORE_OUTPUT);
        }
        if (output instanceof DocumentModel) {
            return attach((DocumentModel) output);
        } else if (output instanceof DocumentModelList) {
            return new DocumentModelListImpl(
                    ((DocumentModelList) output).stream().map(this::attach).collect(Collectors.toList()));
        }
        return output;
    }

    protected DocumentModel attach(DocumentModel doc) {
        String sid = ctx.getCoreSession().getSessionId();
        try {
            doc = doc.clone();
        } catch (CloneNotSupportedException e) {
            // doc models not supporting clone do not rely on session id anyway
        }
        doc.attach(sid);
        return doc;
    }

    protected DocumentModel detach(DocumentModel doc) {
        doc.detach(false);
        return doc;
    }

    protected boolean isAsync(String executionId) {
        return getTransientStore().getParameter(executionId, TRANSIENT_STORE_SERVICE) != null;
    }

    protected Serializable getTaskId(String executionId) {
        return getTransientStore().getParameter(executionId, TRANSIENT_STORE_TASK_ID);
    }

    protected AsyncService getAsyncService(String executionId) {
        try {
            String serviceClass = (String) getTransientStore().getParameter(executionId, TRANSIENT_STORE_SERVICE);
            return (AsyncService) Framework.getService(Class.forName(serviceClass));
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    protected void setCompleted(String executionId) {
        getTransientStore().setCompleted(executionId, true);
    }

    protected boolean isCompleted(String executionId) {
        if (isAsync(executionId)) {
            Serializable taskId = getTransientStore().getParameter(executionId, TRANSIENT_STORE_TASK_ID);
            return getAsyncService(executionId).getStatus(taskId).isCompleted();
        }
        return getTransientStore().isCompleted(executionId);
    }

    protected boolean exists(String executionId) {
        return getTransientStore().exists(executionId);
    }

    protected void cleanup(String executionId) {
        getTransientStore().release(executionId);
    }
}
