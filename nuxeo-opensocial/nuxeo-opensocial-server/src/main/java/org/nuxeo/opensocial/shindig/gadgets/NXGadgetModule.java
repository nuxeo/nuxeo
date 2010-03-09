package org.nuxeo.opensocial.shindig.gadgets;

import org.apache.shindig.gadgets.servlet.MakeRequestHandler;

import com.google.inject.AbstractModule;

/** @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a> */
public class NXGadgetModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(MakeRequestHandler.class).to(NXMakeRequestHandler.class);
    }

}
