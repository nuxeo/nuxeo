/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     mhilaire
 */
package org.nuxeo.ecm.directory.sql;

import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Names;

/**
 * 
 *
 * @since 5.9.6
 */
@RunWith(FeaturesRunner.class)
@Features({ ClientLoginFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.directory.api", "org.nuxeo.ecm.directory",
        "org.nuxeo.ecm.core.schema", "org.nuxeo.ecm.directory.types.contrib",
        "org.nuxeo.ecm.directory.sql" })
public class SQLDirectoryFeature extends SimpleFeature {
    public static final String DEFAULT_TEST_DIRECTORY_NAME = "userDirectory";

    @Inject
    DirectoryService directoryService;

    @Override
    public void configure(final FeaturesRunner runner, Binder binder) {
        bindDirectory(binder, DEFAULT_TEST_DIRECTORY_NAME);
    }

    protected void bindDirectory(Binder binder, final String name) {
        binder.bind(Directory.class).annotatedWith(Names.named(name)).toProvider(
                new Provider<Directory>() {

                    @Override
                    public Directory get() {
                        return Framework.getService(DirectoryService.class).getDirectory(name);
                    }

                });
    }
   
    
    

}
