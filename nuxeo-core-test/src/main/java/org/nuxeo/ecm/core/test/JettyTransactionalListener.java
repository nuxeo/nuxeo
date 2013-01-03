package org.nuxeo.ecm.core.test;

import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jetty.JettyComponent;
import org.nuxeo.runtime.jtajca.NuxeoContainer;

public class JettyTransactionalListener implements ServletContextListener {
        
    JettyComponent component = fetchComponent();
    
    protected static JettyComponent fetchComponent() {
        return (JettyComponent)Framework.getRuntime().getComponent(JettyComponent.NAME);
    }
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            NuxeoContainer.install();
        } catch (NamingException e) {
            throw new RuntimeException(
                    "Cannot install jtajca in jetty naming context", e);
        }
        component.setNuxeoClassLoader(Thread.currentThread().getContextClassLoader());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            NuxeoContainer.uninstall();
        } catch (NamingException e) {
            throw new RuntimeException(
                    "Cannot uninstall jtajca in jetty naming context", e);
        }
    }

}
