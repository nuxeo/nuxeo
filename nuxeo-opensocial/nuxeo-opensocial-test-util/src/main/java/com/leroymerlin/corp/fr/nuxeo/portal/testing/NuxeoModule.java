package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.WebEngine;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.guice.CoreSessionProvider;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.guice.DirectoryServiceProvider;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.guice.RTHarnessProvider;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.guice.SchemaManagerProvider;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.guice.UserManagerProvider;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.guice.WebEngineProvider;

public class NuxeoModule extends AbstractModule {

    /** {@InheritDoc} */
    @Override
    protected void configure() {
        bind(TestRuntimeHarness.class	).toProvider(RTHarnessProvider.class);
        bind(SchemaManager.class).toProvider(SchemaManagerProvider.class).in(Scopes.SINGLETON);
        bind(UserManager.class).toProvider(UserManagerProvider.class).in(Scopes.SINGLETON);
        bind(DirectoryService.class).toProvider(DirectoryServiceProvider.class).in(Scopes.SINGLETON);
        bind(CoreSession.class).toProvider(CoreSessionProvider.class).in(Scopes.SINGLETON);
        bind(WebEngine.class).toProvider(WebEngineProvider.class).in(Scopes.SINGLETON);
    }

}
