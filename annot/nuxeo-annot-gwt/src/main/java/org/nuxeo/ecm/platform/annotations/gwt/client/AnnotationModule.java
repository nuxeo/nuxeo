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
 *
 */
public class AnnotationModule implements EntryPoint {

    private WebConfigurationServiceAsync webConfigurationService;

    private WebConfiguration webConfiguration;

    public void onModuleLoad() {
        fixXMLHttpRequest();
        webConfigurationService = GWT.create(WebConfigurationService.class);
        String url = Window.Location.getHref();
        webConfigurationService.getWebConfiguration(url,
                new AsyncCallback<WebConfiguration>() {
                    public void onFailure(Throwable throwable) {
                        Log.debug("onFailure: " + throwable);
                        webConfiguration = WebConfiguration.DEFAULT_WEB_CONFIGURATION;
                        initModule();
                    }

                    public void onSuccess(WebConfiguration result) {
                        webConfiguration = result == null ? WebConfiguration.DEFAULT_WEB_CONFIGURATION
                                : result;
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
