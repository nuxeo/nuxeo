package org.nuxeo.ecm.webdav;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webdav.provider.ExceptionHandler;
import org.nuxeo.ecm.webdav.provider.WebDavContextResolver;
import org.nuxeo.ecm.webdav.resource.RootResource;

import javax.xml.bind.JAXBException;
import java.util.HashSet;
import java.util.Set;

/**
 * Registers the application (root resource classes and providers)
 * in a standard / container-neutral way.
 */
public class Application extends javax.ws.rs.core.Application {

    private static final Log log = LogFactory.getLog(Application.class);

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(RootResource.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<Object>();
        singletons.add(new ExceptionHandler());
        try {
            singletons.add(new WebDavContextResolver());
        } catch (JAXBException e) {
            log.error(e);
        }
        return singletons;
    }

}
