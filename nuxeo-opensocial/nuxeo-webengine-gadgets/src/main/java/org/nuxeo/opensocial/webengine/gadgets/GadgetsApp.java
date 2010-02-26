package org.nuxeo.opensocial.webengine.gadgets;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

public class GadgetsApp extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> set = new HashSet<Class<?>>();
        set.add(Gadgets.class);
        set.add(GadgetStreamWriter.class);
        return set;
    }

}
