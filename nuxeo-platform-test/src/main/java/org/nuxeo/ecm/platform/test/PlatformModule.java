/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.platform.test;

import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.test.guice.DirectoryServiceProvider;
import org.nuxeo.ecm.platform.test.guice.UserManagerProvider;
import org.nuxeo.ecm.platform.usermanager.UserManager;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/**
 * This Guice module does the necessary bindings for Nuxeo Platform tests.
 */
public class PlatformModule extends AbstractModule {

    @Override
    public void configure() {
        bind(UserManager.class).toProvider(UserManagerProvider.class).in(
                Scopes.SINGLETON);
        bind(DirectoryService.class).toProvider(DirectoryServiceProvider.class).in(
                Scopes.SINGLETON);
    }

}
