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
package org.nuxeo.ecm.directory.ldap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.directory.DirContext;

import org.apache.directory.server.protocol.shared.store.LdifFileLoader;
import org.apache.directory.server.protocol.shared.store.LdifLoadFilter;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.sql.SQLDirectoryFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.SimpleFeature;

import com.google.inject.Binder;
import com.google.inject.Provider;

/**
 * Feature for Embedded LDAP directory unit tests
 *
 * @since 6.0
 */
@Features({ SQLDirectoryFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@LocalDeploy({
        "org.nuxeo.ecm.directory.ldap.tests:ldap-test-setup/LDAPDirectoryFactory.xml",
        "org.nuxeo.ecm.directory.ldap.tests:TestSQLDirectories.xml",
        "org.nuxeo.ecm.directory.ldap.tests:ldap-test-setup/DirectoryTypes.xml" })
public class InternalLDAPDirectoryFeature extends SimpleFeature {

    protected MockLdapServer server;

    // change this flag if your test server has support for dynamic groups
    // through the groupOfURLs objectclass, eg for OpenLDAP:
    // http://www.ldap.org.br/modules/ldap/files/files///dyngroup.schema
    public static final boolean HAS_DYNGROUP_SCHEMA = false;

    @Inject
    @Named(SQLDirectoryFeature.USER_DIRECTORY_NAME)
    Directory userDir;

    @Inject
    @Named(SQLDirectoryFeature.GROUP_DIRECTORY_NAME)
    Directory groupDir;

    @Inject
    DirectoryService dirService;

    public List<String> getLdifFiles() {
        List<String> ldifFiles = new ArrayList<String>();
        ldifFiles.add("sample-users.ldif");
        ldifFiles.add("sample-groups.ldif");
        if (HAS_DYNGROUP_SCHEMA) {
            ldifFiles.add("sample-dynamic-groups.ldif");
        }
        return ldifFiles;
    }

    protected MockLdapServer getEmbeddedLDAP() {
        if (server == null) {
            server = new MockLdapServer(new File("target", "ldap"));
        }
        return server;
    }

    @Override
    public void configure(final FeaturesRunner runner, Binder binder) {
        bindEmbeddedServer(binder);
    }

    protected void bindEmbeddedServer(Binder binder) {
        binder.bind(MockLdapServer.class).toProvider(
                new Provider<MockLdapServer>() {

                    @Override
                    public MockLdapServer get() {
                        return getEmbeddedLDAP();
                    }

                });
    }

    public void stopEmbeddedLDAP() {
        if (server != null) {
            try {
                server.shutdownLdapServer();
            } finally {
                server = null;
            }
        }
    }

    protected void loadDataFromLdif(String ldif, DirContext ctx) {
        List<LdifLoadFilter> filters = new ArrayList<LdifLoadFilter>();
        LdifFileLoader loader = new LdifFileLoader(ctx, new File(ldif),
                filters, Thread.currentThread().getContextClassLoader());
        loader.execute();
    }

}
