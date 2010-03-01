package org.nuxeo.apidoc.browse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

public class ApiDocApplication extends Application {

    @Override
    public Set<Object> getSingletons() {
        return new HashSet<Object>(Arrays.asList(
                new DocumentationItemReader()
                ));
    }


}
