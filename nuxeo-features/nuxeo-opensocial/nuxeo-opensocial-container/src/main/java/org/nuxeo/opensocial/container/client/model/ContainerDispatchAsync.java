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

import net.customware.gwt.dispatch.client.DispatchAsync;
import net.customware.gwt.dispatch.client.service.DispatchService;
import net.customware.gwt.dispatch.client.service.DispatchServiceAsync;
import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;

import org.nuxeo.opensocial.container.client.ContainerConfiguration;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

/**
 * @author Stéphane Fourrier
 */
public class ContainerDispatchAsync implements DispatchAsync {

    private static final DispatchServiceAsync realService = GWT.create(DispatchService.class);

    private static final String DISPATCH_URL_SUFFIX = "gwtContainer/dispatch";

    public ContainerDispatchAsync() {
        String dispatchURL = ContainerConfiguration.getBaseUrl()
                + DISPATCH_URL_SUFFIX;
        ((ServiceDefTarget) realService).setServiceEntryPoint(dispatchURL);
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
