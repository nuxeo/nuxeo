package org.nuxeo.segment.io.web;

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.webengine.app.WebEngineModule;

public class SegmentIOResourceWebApp extends WebEngineModule {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> result = new HashSet<Class<?>>();
        result.add(SegmentIOScriptResource.class);
        return result;
    }

}
