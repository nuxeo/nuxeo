/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.gwt.client.ui.navigator;

import org.nuxeo.ecm.platform.gwt.client.ui.login.LoginDialog;

import com.google.gwt.user.client.Window;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.RestDataSource;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class NuxeoDataSource extends RestDataSource {

    /**
     * Hack to install custom error handler - see RPCManager.js line 3072
     */
    static {
        initErrorHandler();
    }

    public static native void initErrorHandler()/*-{
    $wnd.isc.DataSource.addMethods({
       handleError : function (resp, req) {
       var responseJ = @com.smartgwt.client.data.DSResponse::new(Lcom/google/gwt/core/client/JavaScriptObject;)(resp);
       var requestJ = @com.smartgwt.client.data.DSRequest::new(Lcom/google/gwt/core/client/JavaScriptObject;)(req);
       return @org.nuxeo.ecm.platform.gwt.client.ui.navigator.NuxeoDataSource::handleError(Lcom/smartgwt/client/data/DSResponse;Lcom/smartgwt/client/data/DSRequest;)(responseJ, requestJ);
       }
    });
    }-*/;

    public static Boolean handleError(DSResponse response, DSRequest req) {
        int status = response.getStatus();
        if (status == -7 || response.getHttpResponseCode() == 401) {
            LoginDialog dlg = new LoginDialog();
            dlg.show();
        } else {
            String[] errors = response.getAttributeAsStringArray("data");
            if (errors!= null && errors.length > 0) {
                String msg = "";
                for (String error : errors) {
                    msg = error + "\r\n";
                }
                Window.alert("Server Error. Code: "+status+". HTTP code: "+response.getHttpResponseCode()+"\r\n\r\n"+msg);
            }  else {
                Window.alert("Server Error. Code: "+status+". HTTP code: "+response.getHttpResponseCode());
            }
        }
        return Boolean.FALSE;
    }

}
