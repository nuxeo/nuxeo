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

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

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
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
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

/**
 * Adapter that allows asynchronous execution of operations.
 *
 * @since 10.3
 */
@WebAdapter(name = AsyncOperationAdapter.NAME, type = "AsyncOperationAdapter", targetType = "operation")
@Produces({ MediaType.APPLICATION_JSON })
public class AsyncOperationAdapter extends DefaultAdapter {

    public static final String NAME = "async";

    private static final Logger log = LogManager.getLogger(AsyncOperationAdapter.class);

    protected static final String STATUS_STORE_NAME = "automation";

    protected static final String TRANSIENT_STORE_SERVICE = "service";

    protected static final String TRANSIENT_STORE_TASK_ID = "taskId";

    protected static final String TRANSIENT_STORE_ERROR_STATUS = "status";

    protected static final String TRANSIENT_STORE_ERROR = "error";

    protected static final String TRANSIENT_STORE_OUTPUT = "output";

    protected static final String TRANSIENT_STORE_OUTPUT_BLOB = "blob";

    protected static final String STATUS_PATH= "status";

    protected static final String RUNNING_STATUS= "RUNNING";

    protected static final String RESULT_URL_KEY= "url";

    @Context
    protected AutomationService service;

    @Context
    protected HttpServletRequest request;

    @Context
    protected HttpServletResponse response;

    @Context
    protected CoreSession session;

    @Context
    protected AutomationServer srv;

    @POST
    public Object doPost(ExecutionRequest xreq) {
        OperationResource op = (OperationResource) getTarget();
        String opId = op.getId();

        if (!srv.accept(opId, op.isChain(), request)) {
            return ResponseHelper.notFound();
        }
        String executionId = UUID.randomUUID().toString();

        // session will be set in the task thread
        OperationContext opCtx = xreq.createContext(request, response, null);

        opCtx.setCallback(new OperationCallback() {

            private Object output;

            @Override
            public void onChainEnter(OperationType chain) {
                //
            }

            @Override
            public void onChainExit() {
                setOutput(executionId, (Serializable) output);
                setCompleted(executionId);
            }

            @Override
            public void onOperationEnter(OperationContext context, OperationType type, InvokableMethod method,
                    Map<String, Object> params) {
                enterMethod(executionId, method);
            }

            @Override
            public void onOperationExit(Object output) {
                this.output = output;
            }

            @Override
            public void onError(Exception error) {
                setError(executionId, error);
            }

        });

        String repoName = session.getRepositoryName();
        NuxeoPrincipal principal = session.getPrincipal();

        // TODO NXP-26303: use thread pool
        new Thread(() -> {
            TransactionHelper.runInTransaction(() -> {
                ClientLoginModule.getThreadLocalLogin().push(principal, null, null);
                try (CloseableCoreSession session = CoreInstance.openCoreSession(repoName, principal)){
                    opCtx.setCoreSession(session);
                    service.run(opCtx, opId, xreq.getParams());
                } catch (RuntimeException | OperationException e) {
                    setError(executionId, e);
                } finally {
                    ClientLoginModule.getThreadLocalLogin().pop();
                }
            });
        }, String.format("Nuxeo-AsyncOperation-%s", executionId)).start();

        try {
            String statusURL = String.format("%s%s/%s/%s", ctx.getServerURL(), getPath(), executionId, STATUS_PATH);
            return Response.status(HttpServletResponse.SC_ACCEPTED).location(new URI(statusURL)).build();
        } catch (URISyntaxException e) {
            throw new NuxeoException(e);
        }
    }

    @GET
    @Path("{executionId}/status")
    public Object status(@PathParam("executionId") String executionId) throws IOException, MessagingException {
        if (isCompleted(executionId)) {
            String resURL = String.format("%s/%s", getPath(), executionId);
            return redirect(resURL);
        } else {
            Object result = RUNNING_STATUS;
            Serializable taskId = getAsyncTaskId(executionId);
            if (taskId != null) {
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

            int status = getErrorStatus(executionId);
            String error = getError(executionId);

            // cleanup after result is accessed
            cleanup(executionId);

            if (status != 0) {
                throw new NuxeoException(error, status);
            }

            // if output has a "url" key assume it's a redirect url
            if (output instanceof Map) {
                Object url = ((Map<?, ?>) output).get(RESULT_URL_KEY);
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
            Serializable taskId = getAsyncTaskId(executionId);
            if (taskId != null) {
                return getAsyncService(executionId).abort(taskId);
            }
            return ResponseHelper.getResponse(RUNNING_STATUS, request, HttpServletResponse.SC_OK);
        }
        throw new WebResourceNotFoundException("Execution with id=" + executionId + " has completed");
    }

    protected TransientStore getTransientStore() {
        return Framework.getService(TransientStoreService.class).getStore(STATUS_STORE_NAME);
    }

    protected void enterMethod(String executionId, InvokableMethod method) {
        // reset parameters
        getTransientStore().remove(executionId);

        // AsyncService.class is default => not async
        if (!AsyncService.class.equals(method.getAsyncService())) {
            getTransientStore().putParameter(executionId, TRANSIENT_STORE_SERVICE, method.getAsyncService().getName());
        }
    }

    protected void setError(String executionId, Throwable t) {
        log.error(t);
        // find custom status from any NuxeoException or RestOperationException
        int status = SC_INTERNAL_SERVER_ERROR;
        Throwable cause = t;
        do {
            if (cause instanceof NuxeoException) {
                status = ((NuxeoException) cause).getStatusCode();
            } else if (cause instanceof RestOperationException) {
                status = ((RestOperationException) cause).getStatus();
            }
            cause = cause.getCause();
        } while (cause != null);
        String error;
        if (status < SC_INTERNAL_SERVER_ERROR) {
            error = ExceptionHelper.unwrapException(t).getMessage();
            if (error == null) {
                error = "";
            }
        } else {
            error = "Internal Server Error";
        }
        getTransientStore().putParameter(executionId, TRANSIENT_STORE_ERROR_STATUS, (long) status);
        getTransientStore().putParameter(executionId, TRANSIENT_STORE_ERROR, error);
        setCompleted(executionId);
    }

    public int getErrorStatus(String executionId) {
        Long status = (Long) getTransientStore().getParameter(executionId, TRANSIENT_STORE_ERROR_STATUS);
        return status == null ? 0 : status.intValue();
    }

    public String getError(String executionId) {
        return (String) getTransientStore().getParameter(executionId, TRANSIENT_STORE_ERROR);
    }

    protected void setOutput(String executionId, Serializable output) {
        TransientStore ts = getTransientStore();
        // store only taskId for async tasks
        if (isAsync(executionId)) {
            if (output instanceof AsyncStatus) {
                Serializable taskId = ((AsyncStatus<?>) output).getId();
                ts.putParameter(executionId, TRANSIENT_STORE_TASK_ID, taskId);
            } else if (output != null) {
                log.error("Unexpected output instead of async status: {}", output);
            }
        } else {
            if (output instanceof DocumentModel) {
                detach((DocumentModel) output);
            } else if (output instanceof DocumentModelList) {
                ((DocumentModelList) output).forEach(this::detach);
            }
            if (output instanceof Blob) {
                ts.putParameter(executionId, TRANSIENT_STORE_OUTPUT_BLOB, true);
                ts.putBlobs(executionId, Collections.singletonList((Blob) output));
            } else if (output instanceof BlobList) {
                ts.putParameter(executionId, TRANSIENT_STORE_OUTPUT_BLOB, false);
                ts.putBlobs(executionId, (BlobList) output);
            } else if (output != null) {
                ts.putParameter(executionId, TRANSIENT_STORE_OUTPUT, output);
            }
        }
    }

    protected Object getResult(String executionId) {
        TransientStore ts = getTransientStore();

        Serializable taskId = getAsyncTaskId(executionId);
        if (taskId != null) {
            return getAsyncService(executionId).getResult(taskId);
        }

        Object output;
        List<Blob> blobs = ts.getBlobs(executionId);
        if (CollectionUtils.isNotEmpty(blobs)) {
            boolean isSingle = (boolean) ts.getParameter(executionId, TRANSIENT_STORE_OUTPUT_BLOB);
            output = isSingle ? blobs.get(0) : new BlobList(blobs);
        } else {
            output = ts.getParameter(executionId, TRANSIENT_STORE_OUTPUT);
        }
        if (output instanceof DocumentModel) {
            attach((DocumentModel) output);
        } else if (output instanceof DocumentModelList) {
            ((DocumentModelList) output).forEach(this::attach);
        }
        return output;
    }

    protected void attach(DocumentModel doc) {
        String sid = ctx.getCoreSession().getSessionId();
        doc.attach(sid);
    }

    protected void detach(DocumentModel doc) {
        doc.detach(false);
    }

    protected boolean isAsync(String executionId) {
        return getTransientStore().getParameter(executionId, TRANSIENT_STORE_SERVICE) != null;
    }

    protected Serializable getAsyncTaskId(String executionId) {
        if (isAsync(executionId)) {
            return getTransientStore().getParameter(executionId, TRANSIENT_STORE_TASK_ID);
        }
        return null;
    }

    protected AsyncService getAsyncService(String executionId) {
        String serviceClass = (String) getTransientStore().getParameter(executionId, TRANSIENT_STORE_SERVICE);
        try {
            return (AsyncService) Framework.getService(Class.forName(serviceClass));
        } catch (ClassNotFoundException e) {
            log.error("AsyncService class {} not found", serviceClass);
            return null;
        }
    }

    protected void setCompleted(String executionId) {
        getTransientStore().setCompleted(executionId, true);
    }

    protected boolean isCompleted(String executionId) {
        Serializable taskId = getAsyncTaskId(executionId);
        if (taskId != null) {
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
