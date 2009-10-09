package org.nuxeo.ecm.webengine.test;

import org.nuxeo.ecm.webengine.WebEngine;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class WebengineModule extends AbstractModule {

    public void configure() {
        bind(WebEngine.class).toProvider(WebEngineProvider.class).in(Scopes.SINGLETON);
    }

}
