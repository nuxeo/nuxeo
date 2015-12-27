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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client;

import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.WebConfiguration;
import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.WebConfigurationService;
import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.WebConfigurationServiceAsync;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class AnnotationModule implements EntryPoint {

    private WebConfigurationServiceAsync webConfigurationService;

    private WebConfiguration webConfiguration;

    public void onModuleLoad() {
        fixXMLHttpRequest();
        webConfigurationService = GWT.create(WebConfigurationService.class);
        String url = Window.Location.getHref();
        webConfigurationService.getWebConfiguration(url, new AsyncCallback<WebConfiguration>() {
            public void onFailure(Throwable throwable) {
                Log.debug("onFailure: " + throwable);
                webConfiguration = WebConfiguration.DEFAULT_WEB_CONFIGURATION;
                initModule();
            }

            public void onSuccess(WebConfiguration result) {
                webConfiguration = result == null ? WebConfiguration.DEFAULT_WEB_CONFIGURATION : result;
                initModule();
                Log.debug("Module initialization finished.");
            }
        });
    }

    private void initModule() {
        AnnotationApplication.build(webConfiguration);
    }

    // Fix XMLHttpRequest when using Annotations module on IE6.
    // XMLHttpRequest is defined in $wnd, but not in window (due to sarrisa
    // librairy use in ajax4jsf) so GWT can't instantiate a XMLHttpRequest object.
    private native void fixXMLHttpRequest() /*-{
                                            if ($wnd.XMLHttpRequest && !window.XMLHttpRequest) {
                                            window.XMLHttpRequest = $wnd.XMLHttpRequest;
                                            }
                                            }-*/;

}
