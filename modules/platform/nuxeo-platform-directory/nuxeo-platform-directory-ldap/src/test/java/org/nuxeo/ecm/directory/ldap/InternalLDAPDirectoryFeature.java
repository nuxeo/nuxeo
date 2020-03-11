/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     mhilaire
 */
package org.nuxeo.ecm.directory.ldap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.naming.directory.DirContext;

import org.apache.directory.server.protocol.shared.store.LdifFileLoader;
import org.apache.directory.server.protocol.shared.store.LdifLoadFilter;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.sql.SQLDirectoryFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

import com.google.inject.Binder;

/**
 * Feature for Embedded LDAP directory unit tests
 *
 * @since 6.0
 */
@Features({ SQLDirectoryFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.directory.ldap")
@Deploy("org.nuxeo.ecm.directory.ldap.tests:TestSQLDirectories.xml")
@Deploy("org.nuxeo.ecm.directory.ldap.tests:ldap-test-setup/DirectoryTypes.xml")
public class InternalLDAPDirectoryFeature implements RunnerFeature {

    protected MockLdapServer server;

    // change this flag if your test server has support for dynamic groups
    // through the groupOfURLs objectclass, eg for OpenLDAP:
    // http://www.ldap.org.br/modules/ldap/files/files///dyngroup.schema
    public static final boolean HAS_DYNGROUP_SCHEMA = false;

    public List<String> getLdifFiles() {
        List<String> ldifFiles = new ArrayList<>();
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
        binder.bind(MockLdapServer.class).toProvider(this::getEmbeddedLDAP);
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
        List<LdifLoadFilter> filters = new ArrayList<>();
        LdifFileLoader loader = new LdifFileLoader(ctx, new File(ldif), filters,
                Thread.currentThread().getContextClassLoader());
        loader.execute();
    }

}
