package org.nuxeo.webengine.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.SimplePanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Main implements EntryPoint {

    protected SimplePanel contentPanel;

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        
//        new ApplicationWindowImpl().register();
//        new ViewContainerImpl().register();
//        new EditorContainerImpl().register();
        
        Application.start();
        GWT.log("Application started", null);
    }

}
