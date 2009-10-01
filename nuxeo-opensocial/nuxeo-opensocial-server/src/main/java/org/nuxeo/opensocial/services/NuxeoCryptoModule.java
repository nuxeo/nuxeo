package org.nuxeo.opensocial.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shindig.auth.SecurityTokenDecoder;
import org.nuxeo.opensocial.shindig.crypto.NXSecurityTokenDecoder;

import com.google.inject.AbstractModule;

public class NuxeoCryptoModule extends AbstractModule {

    private static final Log LOG = LogFactory.getLog(NuxeoCryptoModule.class);

    @Override
    protected final void configure() {
        try {
            bind(SecurityTokenDecoder.class).to(NXSecurityTokenDecoder.class);
        } catch (Exception e) {
            LOG.error("Unable to bind Shindig services to Nuxeo components");
            LOG.error(e.getMessage());
        }
    }

}
