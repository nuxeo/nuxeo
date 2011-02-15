package org.nuxeo.opensocial.container.client.model;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import net.customware.gwt.dispatch.client.DispatchAsync;
import net.customware.gwt.dispatch.client.service.DispatchService;
import net.customware.gwt.dispatch.client.service.DispatchServiceAsync;
import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;

/**
 * @author Stéphane Fourrier
 */
public class ContainerDispatchAsync implements DispatchAsync {

    private static final DispatchServiceAsync realService = GWT.create(DispatchService.class);

    private static final String dispatchUrl = "/nuxeo/gwtContainer/dispatch";

    public ContainerDispatchAsync() {
        ((ServiceDefTarget) realService).setServiceEntryPoint(dispatchUrl);
    }

    public <A extends Action<R>, R extends Result> void execute(final A action,
            final AsyncCallback<R> callback) {
        realService.execute(action, new AsyncCallback<Result>() {
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            @SuppressWarnings("unchecked")
            public void onSuccess(Result result) {
                callback.onSuccess((R) result);
            }
        });
    }

}
