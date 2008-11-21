package org.nuxeo.ecm.webengine.gwt.client;



import org.nuxeo.ecm.webengine.gwt.client.test.TestBundle;
import org.nuxeo.ecm.webengine.gwt.client.ui.impl.DefaultApplicationBundle;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Main implements EntryPoint {


    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        
        ApplicationBundle appBundle = GWT.create(DefaultApplicationBundle.class);        
        appBundle.deploy();
        appBundle = GWT.create(TestBundle.class);
        appBundle.deploy();        
        
        
        Application.debugStart("http://localhost:8888/redirect/skin/wiki/css/wiki.css");
        
        GWT.log("Application started", null);
    }

}
