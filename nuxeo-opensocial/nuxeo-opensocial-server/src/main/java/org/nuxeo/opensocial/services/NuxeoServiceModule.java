package org.nuxeo.opensocial.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shindig.social.opensocial.spi.ActivityService;
import org.apache.shindig.social.opensocial.spi.AppDataService;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.AbstractModule;

public class NuxeoServiceModule extends AbstractModule {

    private static final Log LOG = LogFactory.getLog(NuxeoServiceModule.class);

    @Override
    protected final void configure() {

        try {
            bind(PersonService.class).toInstance(
                    Framework.getService(PersonService.class));
            bind(ActivityService.class).toInstance(
                    Framework.getService(ActivityService.class));
            bind(AppDataService.class).toInstance(
                    Framework.getService(AppDataService.class));

        } catch (Exception e) {
            LOG.error("Unable to bind Shindig services to Nuxeo components");
            LOG.error(e.getMessage());
        }

    }

}
