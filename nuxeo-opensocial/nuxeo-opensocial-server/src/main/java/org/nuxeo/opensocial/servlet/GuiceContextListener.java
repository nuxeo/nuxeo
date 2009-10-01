package org.nuxeo.opensocial.servlet;

import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.tools.jmx.Manager;

public class GuiceContextListener implements ServletContextListener {
    public static final String INJECTOR_ATTRIBUTE = "guice-injector";

    public static final String MODULES_ATTRIBUTE = "guice-modules";

    private boolean jmxInitialized = false;

    private static final Log log = LogFactory.getLog(GuiceContextListener.class);

    public void contextInitialized(ServletContextEvent event) {
        log.info("GuiceContextListener contextInitialized");
        ServletContext context = event.getServletContext();

        List<Module> modules = getModuleList(context.getInitParameter(MODULES_ATTRIBUTE));
        log.info("GuiceContextListener getModuleList");
        Injector injector = null;
        try {
            OpenSocialService service = Framework.getService(OpenSocialService.class);
            log.info("GuiceContextListener createInjector");
            injector = Guice.createInjector(Stage.PRODUCTION, modules);
            service.setInjector(injector);
            context.setAttribute(INJECTOR_ATTRIBUTE, injector);

        } catch (Exception e) {
            log.error("GuiceContextListener caught"
                    + " exception during injection process", e);
            throw new RuntimeException(e);
        }

        try {
            if (!jmxInitialized) {
                Manager.manage("ShindigGuiceContext", injector);
                jmxInitialized = true;
            }
        } catch (Exception e) {
            log.error("GuiceContextListener caught exception "
                    + "trying to init shindig guice context (JMX):", e);
        }
    }

    private Module getModuleInstance(String moduleName)
            throws InstantiationException {
        try {
            return (Module) Class.forName(moduleName).newInstance();
        } catch (IllegalAccessException e) {
            throw new InstantiationException();
        } catch (ClassNotFoundException e) {
            throw new InstantiationException();
        }
    }

    private List<Module> getModuleList(String moduleNames) {
        List<Module> modules = new LinkedList<Module>();
        if (moduleNames != null) {
            for (String moduleName : moduleNames.split(":")) {
                try {
                    moduleName = moduleName.trim();
                    if (moduleName.length() > 0) {
                        Module moduleInstance = getModuleInstance(moduleName);
                        modules.add(moduleInstance);
                    }
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return modules;
    }

    public void contextDestroyed(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        context.removeAttribute(INJECTOR_ATTRIBUTE);
    }
}
