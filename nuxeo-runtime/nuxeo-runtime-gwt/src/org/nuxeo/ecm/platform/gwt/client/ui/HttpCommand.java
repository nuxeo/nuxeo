/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.gwt.client.ui;

import org.nuxeo.ecm.platform.gwt.client.Framework;
import org.nuxeo.ecm.platform.gwt.client.http.HttpCallback;
import org.nuxeo.ecm.platform.gwt.client.http.HttpRequest;
import org.nuxeo.ecm.platform.gwt.client.http.HttpResponse;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.RequestBuilder.Method;
import com.google.gwt.user.client.Command;

/**
 * TODO: busy timeout is not propagated to UI.showBusy
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class HttpCommand extends HttpCallback implements Command {

    protected View view;
    /**
     *  the timeout to wait before starting busy state.
     *  If -1 busy state is disabled.
     *  You should use a value >=0 for long time running commands 
     */     
    protected int busyTimeout = -1;

    public HttpCommand() {
        this (null, -1);
    }

    public HttpCommand(int busyTimeout) {
        this (null, -1);
    }
    
    public HttpCommand(View view) {
        this (view, -1);
    }

    public HttpCommand(View view, int busyTimeout) {
        this.view = view;
        this.busyTimeout = busyTimeout;
    }

    public View getView() {
        return view;
    }
    
    public HttpCommand setView(View view) {
        this.view = view;
        return this;
    }

    /**
     * @return the busyTimeout.
     */
    public int getBusyTimeout() {
        return busyTimeout;
    }
    
    /**
     * @param busyTimeout the busyTimeout to set.
     */
    public HttpCommand  setBusyTimeout(int busyTimeout) {
        this.busyTimeout = busyTimeout;
        return this;
    }
    
    public void execute() {
        try {
            doExecute();
        } catch (Throwable t) {
            Framework.handleError(t);
        }
    }
    
    protected abstract void doExecute() throws Throwable;
    
    
    public HttpRequest get(String url) {
        return new CommandRequest(RequestBuilder.GET, url);
    }

    public HttpRequest post(String url) {
        return new CommandRequest(RequestBuilder.POST, url); 
    }

    
    @Override
    public void onError(Request request, Throwable exception) {
        hideBusy();
        super.onError(request, exception);
    }
    
    @Override
    public void onResponseReceived(Request request, Response response) {
        hideBusy();
        super.onResponseReceived(request, response);
    }
    
    public void onSuccess(HttpResponse response) {
    }
    
    public void onFailure(Throwable cause) {
        Framework.handleError(cause);
    }
    
    protected void showBusy() {
        if (busyTimeout > -1) {
            if (view != null) {
                view.showBusy();
            } else {
                UI.showBusy();
            }
        }
    }
    
    protected void hideBusy() {
        if (busyTimeout > -1) {
            if (view != null) {
                view.hideBusy();
            } else {
                UI.hideBusy();
            }        
        }
    }

    class CommandRequest extends HttpRequest {      
        public CommandRequest(Method method, String url) {
            super(method, url);
        }
        @Override
        public Request send() throws RequestException {
            setHeader("X-Requested-With", "gwt");
            setCallback(HttpCommand.this);
            showBusy();
            return super.send();
        }
        
        @Override
        public Request sendRequest(String requestData, RequestCallback callback)
                throws RequestException {
            try {
                setHeader("X-Requested-With", "gwt");
                showBusy();
                return super.sendRequest(requestData, callback);
            } catch (RequestException e) {
                hideBusy();
                throw e;
            }
        }
    }
    
}
