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

package org.nuxeo.ecm.webengine.gwt.client.ui;

import org.nuxeo.ecm.webengine.gwt.client.http.HttpCallback;
import org.nuxeo.ecm.webengine.gwt.client.http.HttpRequest;
import org.nuxeo.ecm.webengine.gwt.client.http.HttpResponse;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.RequestBuilder.Method;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ViewRequest extends HttpRequest {

    protected View view;

    public ViewRequest(View item, Method method, String url) {
        super (method, url);
        this.view = item;
    }

    public View getView() {
        return view;
    }

    @Override
    public Request send() throws RequestException {
        view.showBusy();
        setCallback(new Callback());
        return super.send();
    }

    @Override
    public Request sendRequest(String requestData, RequestCallback callback)
            throws RequestException {
        view.showBusy();
        setCallback(new Callback());
        return super.sendRequest(requestData, callback);
    }


    class Callback extends HttpCallback {
        @Override
        public void onSuccess(HttpResponse response) {
            view.onRequestSuccess(getRequest(), response);
        }
        public void onFailure(Throwable cause) {
            view.onRequestFailure(getRequest(), cause);
        }
    }

}
