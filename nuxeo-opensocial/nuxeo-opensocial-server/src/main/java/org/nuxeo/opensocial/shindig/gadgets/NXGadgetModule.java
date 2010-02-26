package org.nuxeo.opensocial.shindig.gadgets;

import com.google.inject.AbstractModule;

/** @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a> */
public class NXGadgetModule extends AbstractModule {

    @Override
    protected void configure() {
        // bind(GadgetSpecFactory.class).to(NXGadgetSpecFactory.class).in(
        // Scopes.SINGLETON);
        // Map<String, String> nuxeo = new HashMap<String, String>();
        // nuxeo.put("OAUTH_SIGNING_KEY_FILE",
        // "/Users/iansmith/googledocs/nuxeo-source/nuxeo");
        // nuxeo.put("OAUTH_SIGNING_KEY_NAME", "nuxeo opensocial");
        // Names.bindProperties(binder(), nuxeo);
    }

}
