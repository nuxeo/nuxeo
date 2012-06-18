package org.nuxeo.template.xdocreport.jaxrs;

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.webengine.app.WebEngineModule;

import fr.opensagres.xdocreport.remoting.resources.services.rest.Providers;

/**
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
public class JAXRSRemotingApplication extends WebEngineModule {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> result = new HashSet<Class<?>>();
        result.add(RootResource.class);
        return result;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> result = new HashSet<Object>();
        result.add(new ResourceMessageWriter());
        result.addAll(Providers.get());
        return result;
    }

}
