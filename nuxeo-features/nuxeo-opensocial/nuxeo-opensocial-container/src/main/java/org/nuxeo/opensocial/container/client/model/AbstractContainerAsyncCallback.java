package org.nuxeo.opensocial.container.client.model;

import org.nuxeo.opensocial.container.client.event.priv.app.SendMessageEvent;
import org.nuxeo.opensocial.container.client.utils.Severity;

import com.google.gwt.user.client.rpc.AsyncCallback;

import net.customware.gwt.presenter.client.EventBus;

/**
 * @author Stéphane Fourrier
 */
abstract public class AbstractContainerAsyncCallback<T> implements
        AsyncCallback<T> {
    private String errorMessage;

    private EventBus eventBus;

    public AbstractContainerAsyncCallback(EventBus eventBus, String errorMessage) {
        this.errorMessage = errorMessage;
        this.eventBus = eventBus;
    }

    public void onSuccess(T result) {
        if (result == null) {
            throwErrorMessage();
        } else {
            doExecute(result);
        }
    }

    public void onFailure(Throwable caught) {
        throwErrorMessage();
    }

    private void throwErrorMessage() {
        eventBus.fireEvent(new SendMessageEvent(errorMessage, Severity.ERROR));
    }

    abstract protected void doExecute(T result);
}
