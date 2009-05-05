package org.nuxeo.ecm.platform.annotations.gwt.client;

import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.WebConfiguration;
import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.WebConfigurationService;
import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.WebConfigurationServiceAsync;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class AnnotationFrameModule implements EntryPoint {

    private WebConfigurationServiceAsync webConfigurationService;

    private WebConfiguration webConfiguration;

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        waitForAnnoteaServerUrlRegistered();
    }

    private void waitForAnnoteaServerUrlRegistered() {
        Timer timer = new Timer() {
            @Override
            public void run() {
                if (isAnnoteaServerUrlRegistered()) {
                    loadModule();
                } else {
                    schedule(200);
                }
            }
        };
        timer.schedule(200);
    }

    private void loadModule() {
        webConfigurationService = GWT.create(WebConfigurationService.class);
        String url = getParentWindowUrl();
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

    private native boolean isAnnoteaServerUrlRegistered() /*-{
        if (top['annoteaServerUrlRegistered']) {
            return top['annoteaServerUrlRegistered'];
        }
        return false;
    }-*/;

    private native String getParentWindowUrl() /*-{
        return $wnd.parent.location.href;
    }-*/;

    private void initModule() {
        AnnotationFrameApplication.build(webConfiguration);
    }

}
