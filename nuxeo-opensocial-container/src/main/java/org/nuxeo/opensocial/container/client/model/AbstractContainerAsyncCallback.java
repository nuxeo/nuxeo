/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stéphane Fourrier
 */

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
