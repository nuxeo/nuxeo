package org.nuxeo.ecm.core.test;

import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.nuxeo.runtime.jtajca.NuxeoContainer;

public class JettyTransactionalListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            NuxeoContainer.install();
        } catch (NamingException e) {
            throw new Error("Cannot install jtajca in jetty naming context", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            NuxeoContainer.uninstall();
        } catch (NamingException e) {
            throw new Error("Cannot uninstall jtajca in jetty naming context", e);
        }
    }

}
