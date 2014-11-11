/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.opensocial.container.client.view.rest;

import org.nuxeo.opensocial.container.client.JsLibrary;
import org.nuxeo.opensocial.container.client.view.GadgetForm;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.gwtext.client.widgets.form.Field;
import com.gwtext.client.widgets.form.TextArea;

public class NXRequestCallback implements RequestCallback {

    private Field field;

    private NXDataWindow nxDataWindow;

    private TextArea fieldHidden;

    private String type;

    public NXRequestCallback(Field field, TextArea fieldHidden, String type) {
        this.field = field;
        this.fieldHidden = fieldHidden;
        this.type = type;
    }

    public NXRequestCallback(NXDataWindow nxDataWindow) {
        this.nxDataWindow = nxDataWindow;
    }

    public void onError(Request arg0, Throwable arg1) {
        JsLibrary.log("error");
    }

    public void onResponseReceived(Request arg0, Response rep) {
        JSONObject o = JSONParser.parse(rep.getText()).isObject();
        JSONObject summary = o.get("summary").isObject();

        if (nxDataWindow != null) {
            nxDataWindow.setDataView(o.get("data").isArray(), summary.get(
                    "pageNumber").isNumber().doubleValue(),
                    summary.get("pages").isNumber().doubleValue());
        } else {
            new NXDataWindow(o.get("data").isArray(), GadgetForm.window,
                    this.field, this.fieldHidden,
                    summary.get("pageNumber").isNumber().doubleValue(),
                    summary.get("pages").isNumber().doubleValue(), this.type);
        }
    }
}
