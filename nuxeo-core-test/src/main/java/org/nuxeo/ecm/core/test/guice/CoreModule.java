/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 * $Id$
 */
package org.nuxeo.ecm.core.test.guice;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.test.NuxeoCoreRunner;
import org.nuxeo.ecm.core.test.RepoType;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;


public class CoreModule extends AbstractModule {

    public void configure() {
        bind(RepoType.class).toProvider((NuxeoCoreRunner)NuxeoCoreRunner.getInstance());
        bind(SchemaManager.class).toProvider(SchemaManagerProvider.class).in(Scopes.SINGLETON);
        bind(CoreSession.class).toProvider(CoreSessionProvider.class).in(Scopes.SINGLETON);

    }

}
