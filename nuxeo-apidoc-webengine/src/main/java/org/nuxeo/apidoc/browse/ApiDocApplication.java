package org.nuxeo.apidoc.browse;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

public class ApiDocApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> result = new HashSet<Class<?>>();
        result.add(DocumentationItemReader.class);
        return result;
    }
    

}
