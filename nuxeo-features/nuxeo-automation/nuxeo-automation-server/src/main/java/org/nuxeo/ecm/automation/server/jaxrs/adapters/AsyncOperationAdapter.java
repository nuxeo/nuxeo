package org.nuxeo.ecm.automation.server.jaxrs.adapters;

import java.io.Serializable;
import java.net.URI;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
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
import org.nuxeo.ecm.automation.OperationCallback;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.impl.InvokableMethod;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.jaxrs.DefaultJsonAdapter;
import org.nuxeo.ecm.automation.jaxrs.io.operations.ExecutionRequest;
import org.nuxeo.ecm.automation.server.AutomationServer;
import org.nuxeo.ecm.automation.server.jaxrs.OperationResource;
import org.nuxeo.ecm.automation.server.jaxrs.ResponseHelper;
import org.nuxeo.ecm.automation.server.jaxrs.RestOperationException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.ExceptionHelper;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.runtime.api.Framework;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@WebAdapter(name = AsyncOperationAdapter.NAME, type = "AsyncOperationAdapter", targetType = "operation")
@Produces({ MediaType.APPLICATION_JSON })
public class AsyncOperationAdapter extends DefaultAdapter {

    public static final String NAME = "async";

    public static final String STATUS_STORE_NAME = "automation";

    String TRANSIENT_STORE_PARAM_ERROR = "error";

    String TRANSIENT_STORE_PARAM_OUTPUT = "output";

    @Context
    protected HttpServletRequest request;

    @POST
    public Object doPost(ExecutionRequest xreq) {
        OperationResource op = (OperationResource) getTarget();

        try {
            AutomationServer srv = Framework.getService(AutomationServer.class);
            if (!srv.accept(op.getId(), op.isChain(), request)) {
                return ResponseHelper.notFound();
            }
            String executionId = UUID.randomUUID().toString();

            // XXX: AsyncContext asyncContext = ctx.getRequest().startAsync();
            new Thread(() -> {
                try {
                    op.execute(xreq, new OperationCallback() {

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
                                Map<String, Object> parms) {
                            //
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
                } catch (OperationException e) {
                    setError(executionId, e.getMessage());
                }
            }).run();

            String statusURL = String.format("%s/%s/status", ctx.getURL(), executionId);
            return Response.status(Response.Status.ACCEPTED).location(new URI(statusURL)).build();

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

        Serializable output = getOutput(executionId);

        if (!isCompleted(executionId)) {
            return ResponseHelper.getResponse("RUNNING", request, Response.Status.OK.getStatusCode());
        }

        if (output instanceof BulkStatus) {
            return getBulkActionResponse((BulkStatus) output);
        } else {
            String resURL = StringUtils.stripEnd(ctx.getURL(), "/status");
            return Response.status(Response.Status.SEE_OTHER).location(new URI(resURL)).build();
        }
    }

    @GET
    @Path("{executionId}")
    public Object result(@PathParam("executionId") String executionId)
            throws IOException, URISyntaxException, MessagingException {

        Serializable output = getOutput(executionId);

        if (isCompleted(executionId)) {
            if (output instanceof BulkStatus) {
                return getBulkActionResponse((BulkStatus) output);
            } else {
                return ResponseHelper.getResponse(output, request, Response.Status.OK.getStatusCode());
            }
        }

        throw new WebResourceNotFoundException("Execution with id=" + executionId + " not found");
    }

    @DELETE
    @Path("{executionId}")
    public Object abort(@PathParam("executionId") String executionId) throws IOException, MessagingException {
        Serializable output = getOutput(executionId);

        if (exists(executionId) && isCompleted(executionId)) {
            if (output instanceof BulkStatus) {
                BulkStatus status = (BulkStatus) output;
                String commandId = status.getCommandId();
                status = Framework.getService(BulkService.class).abort(executionId);
                if (status.getState() == BulkStatus.State.UNKNOWN) {
                    throw new WebResourceNotFoundException("Command with id=" + commandId + " doesn't exist");
                }
                return status;
            }
            return ResponseHelper.getResponse("RUNNING", request, Response.Status.OK.getStatusCode());
        }
        throw new WebResourceNotFoundException("Execution with id=" + executionId + " has completed");
    }

    /**
     * @since 10.3
     */
    public TransientStore getTransientStore() {
        return Framework.getService(TransientStoreService.class).getStore(STATUS_STORE_NAME);
    }

    public void setError(String executionId, String error) {
        getTransientStore().putParameter(executionId, TRANSIENT_STORE_PARAM_ERROR, error);
    }

    public String getError(String executionId) {
        return (String) getTransientStore().getParameter(executionId, TRANSIENT_STORE_PARAM_ERROR);
    }

    public void setOutput(String executionId, Serializable output) {
        if (output instanceof DocumentModel) {
            output = detach((DocumentModel) output);
        } else if (output instanceof DocumentModelList) {
            output = new DocumentModelListImpl(
                    ((DocumentModelList) output).stream().map(this::detach).collect(Collectors.toList()));
        }
        if (output instanceof Blob) {
            getTransientStore().putBlobs(executionId, Collections.singletonList((Blob) output));
        } else if (output instanceof BlobList) {
            getTransientStore().putBlobs(executionId, (BlobList) output);
        } else {
            getTransientStore().putParameter(executionId, TRANSIENT_STORE_PARAM_OUTPUT, output);
        }
    }

    public Serializable getOutput(String executionId) {
        Object output;
        List<Blob> blobs = getTransientStore().getBlobs(executionId);
        if (CollectionUtils.isNotEmpty(blobs)) {
            // XXX: handle single blob result better
            output = blobs.size() == 1 ? blobs.get(0) : new BlobList(blobs);
        } else {
            output = getTransientStore().getParameter(executionId, TRANSIENT_STORE_PARAM_OUTPUT);
        }
        if (output instanceof DocumentModel) {
            return attach((DocumentModel) output);
        } else if (output instanceof DocumentModelList) {
            return new DocumentModelListImpl(
                    ((DocumentModelList) output).stream().map(this::attach).collect(Collectors.toList()));
        }
        return (Serializable) output;
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

    public void setCompleted(String executionId) {
        getTransientStore().setCompleted(executionId, true);
    }

    public boolean isCompleted(String executionId) {
        return getTransientStore().isCompleted(executionId);
    }

    public boolean exists(String executionId) {
        return getTransientStore().exists(executionId);
    }

    protected Response getBulkActionResponse(BulkStatus status) throws URISyntaxException {
        String commandId = status.getCommandId();
        status = Framework.getService(BulkService.class).getStatus(commandId);
        if (status.getState() == BulkStatus.State.UNKNOWN) {
            throw new WebResourceNotFoundException("Command with id=" + commandId + " doesn't exist");
        }
        // if status is complete and we have a url in the result
        if (status.getState() == BulkStatus.State.COMPLETED) {
            if (status.getResult().containsKey("url")) {
                URI uri = new URI((String) status.getResult().get("url"));
                return Response.status(Response.Status.SEE_OTHER).location(uri).build();
            }
        }
        return Response.status(Response.Status.OK).entity(new DefaultJsonAdapter(status)).build();
    }
}
