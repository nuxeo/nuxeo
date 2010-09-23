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
        Set<Class<?>> set = new HashSet<Class<?>>();
        set.add(RootResource.class);
        return set;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> set = new HashSet<Object>();
        set.add(new ExceptionHandler());
        try {
            set.add(new WebDavContextResolver());
        } catch (JAXBException e) {
            log.error(e);
        }
        return set;
    }

}
