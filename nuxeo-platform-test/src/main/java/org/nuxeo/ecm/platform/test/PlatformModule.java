package org.nuxeo.ecm.platform.test;

import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.test.guice.DirectoryServiceProvider;
import org.nuxeo.ecm.platform.test.guice.UserManagerProvider;
import org.nuxeo.ecm.platform.usermanager.UserManager;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;


public class PlatformModule extends AbstractModule {

    public void configure() {
        bind(UserManager.class).toProvider(UserManagerProvider.class).in(Scopes.SINGLETON);
        bind(DirectoryService.class).toProvider(DirectoryServiceProvider.class).in(Scopes.SINGLETON);


    }

}
