package org.nuxeo.ecm.webdav;

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.webdav.provider.WebDavContextResolver;
import org.nuxeo.ecm.webdav.provider.ExceptionHandler;
import org.nuxeo.ecm.webdav.resource.RootResource;

import javax.xml.bind.JAXBException;

/**
 * Used to register the application (root resource classes and providers) in a container-neutral way.
 */
public class Application extends javax.ws.rs.core.Application {

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
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return set;
    }

}
