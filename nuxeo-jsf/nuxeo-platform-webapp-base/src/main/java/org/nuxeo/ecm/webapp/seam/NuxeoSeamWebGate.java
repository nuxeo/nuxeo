package org.nuxeo.ecm.webapp.seam;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class NuxeoSeamWebGate implements ServletContextListener {
    protected static NuxeoSeamWebGate instance;
    
    protected boolean initialized;
    
    public NuxeoSeamWebGate() {
        instance = this;
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        initialized = false;
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
       initialized = true; 
    }

    public static boolean isInitialized() {
        return instance != null && instance.initialized;
    }
}
