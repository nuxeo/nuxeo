package org.nuxeo.opensocial.shindig.gadgets;

import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.servlet.MakeRequestHandler;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/** @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a> */
public class NXGadgetModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(MakeRequestHandler.class).to(NXMakeRequestHandler.class);
        bind(HttpFetcher.class).to(NXHttpFetcher.class).in(Scopes.SINGLETON);
    }

}
