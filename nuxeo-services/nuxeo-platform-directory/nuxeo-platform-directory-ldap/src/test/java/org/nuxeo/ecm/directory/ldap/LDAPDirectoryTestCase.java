/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.directory.ldap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.SizeLimitExceededException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.directory.server.protocol.shared.store.LdifFileLoader;
import org.apache.directory.server.protocol.shared.store.LdifLoadFilter;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="ogrisel@nuxeo.com">Olivier Grisel</a>
 */
public abstract class LDAPDirectoryTestCase extends NXRuntimeTestCase {

    private static final Log log = LogFactory.getLog(LDAPDirectoryTestCase.class);

    protected static final MockLdapServer SERVER = new MockLdapServer();

    // change this flag to use an external LDAP directory instead of the
    // non networked default ApacheDS implementation
    public static final boolean USE_EXTERNAL_TEST_LDAP_SERVER = false;

    // change this flag in case the external LDAP server considers the
    // posixGroup class structural
    public static final boolean POSIXGROUP_IS_STRUCTURAL = true;

    // change this flag if your test server has support for dynamic groups
    // through the groupOfURLs objectclass, eg for OpenLDAP:
    // http://www.ldap.org.br/modules/ldap/files/files///dyngroup.schema
    public static final boolean HAS_DYNGROUP_SCHEMA = false;

    public static final String INTERNAL_SERVER_SETUP_UPPER_ID = "TestDirectoriesWithInternalApacheDS-override-upper-id.xml";

    // These variables are changed in subclasses
    public String EXTERNAL_SERVER_SETUP = "TestDirectoriesWithExternalOpenLDAP.xml";

    public String INTERNAL_SERVER_SETUP = "TestDirectoriesWithInternalApacheDS.xml";

    public String EXTERNAL_SERVER_SETUP_OVERRIDE = "TestDirectoriesWithExternalOpenLDAP-override.xml";

    public String INTERNAL_SERVER_SETUP_OVERRIDE = "TestDirectoriesWithInternalApacheDS-override.xml";

    public List<String> getLdifFiles() {
        List<String> ldifFiles = new ArrayList<String>();
        ldifFiles.add("sample-users.ldif");
        ldifFiles.add("sample-groups.ldif");
        if (HAS_DYNGROUP_SCHEMA) {
            ldifFiles.add("sample-dynamic-groups.ldif");
        }
        return ldifFiles;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        DatabaseHelper.DATABASE.setUp();

        // setup the client environment
        deployContrib("org.nuxeo.ecm.core", "OSGI-INF/CoreService.xml");
        deployContrib("org.nuxeo.ecm.directory.ldap.tests",
                "ldap-test-setup/TypeService.xml");

        deployContrib("org.nuxeo.ecm.directory.ldap.tests",
                "ldap-test-setup/DirectoryTypes.xml");
        deployContrib("org.nuxeo.ecm.directory.ldap.tests",
                "ldap-test-setup/DirectoryService.xml");
        deployContrib("org.nuxeo.ecm.directory.ldap.tests",
                "ldap-test-setup/LDAPDirectoryFactory.xml");
        deployContrib("org.nuxeo.ecm.directory.sql",
                "OSGI-INF/SQLDirectoryFactory.xml");
        deployContrib("org.nuxeo.ecm.directory.ldap.tests",
                "TestSQLDirectories.xml");
        if (USE_EXTERNAL_TEST_LDAP_SERVER) {
            deployContrib("org.nuxeo.ecm.directory.ldap.tests",
                    EXTERNAL_SERVER_SETUP);
        } else {
            deployContrib("org.nuxeo.ecm.directory.ldap.tests",
                    INTERNAL_SERVER_SETUP);
            getLDAPDirectory("userDirectory").setTestServer(SERVER);
            getLDAPDirectory("groupDirectory").setTestServer(SERVER);
        }
        LDAPSession session = (LDAPSession) getLDAPDirectory("userDirectory").getSession();
        try {
            DirContext ctx = session.getContext();
            for (String ldifFile : getLdifFiles()) {
                loadDataFromLdif(ldifFile, ctx);
            }
        } finally {
            session.close();
        }
    }

    @Override
    public void tearDown() throws Exception {
        LDAPSession session = (LDAPSession) getLDAPDirectory("userDirectory").getSession();
        try {
            if (USE_EXTERNAL_TEST_LDAP_SERVER) {
                DirContext ctx = session.getContext();
                destroyRecursively("ou=people,dc=example,dc=com", ctx, -1);
                destroyRecursively("ou=groups,dc=example,dc=com", ctx, -1);
            } else {
                DirContext ctx = SERVER.getContext();
                destroyRecursively("ou=people", ctx, -1);
                destroyRecursively("ou=groups", ctx, -1);
            }
        } finally {
            session.close();
        }
        undeployContrib("org.nuxeo.ecm.core", "OSGI-INF/CoreService.xml");
        undeployContrib("org.nuxeo.ecm.directory.ldap.tests",
                "ldap-test-setup/TypeService.xml");

        undeployContrib("org.nuxeo.ecm.directory.ldap.tests",
                "ldap-test-setup/DirectoryTypes.xml");
        undeployContrib("org.nuxeo.ecm.directory.ldap.tests",
                "ldap-test-setup/DirectoryService.xml");
        undeployContrib("org.nuxeo.ecm.directory.ldap.tests",
                "ldap-test-setup/LDAPDirectoryFactory.xml");
        undeployContrib("org.nuxeo.ecm.directory.sql",
                "OSGI-INF/SQLDirectoryFactory.xml");
        undeployContrib("org.nuxeo.ecm.directory.ldap.tests",
                "TestSQLDirectories.xml");
        if (USE_EXTERNAL_TEST_LDAP_SERVER) {
            undeployContrib("org.nuxeo.ecm.directory.ldap.tests",
                    EXTERNAL_SERVER_SETUP);
        } else {
            undeployContrib("org.nuxeo.ecm.directory.ldap.tests",
                    INTERNAL_SERVER_SETUP);
        }
        DatabaseHelper.DATABASE.tearDown();
        super.tearDown();
    }

    protected static void loadDataFromLdif(String ldif, DirContext ctx) {
        List<LdifLoadFilter> filters = new ArrayList<LdifLoadFilter>();
        LdifFileLoader loader = new LdifFileLoader(ctx, new File(ldif),
                filters, Thread.currentThread().getContextClassLoader());
        loader.execute();
    }

    protected void destroyRecursively(String dn, DirContext ctx, int limit)
            throws NamingException {
        if (limit == 0) {
            log.warn("Reach recursion limit, stopping deletion at" + dn);
            return;
        }
        SearchControls scts = new SearchControls();
        scts.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        String providerUrl = (String) ctx.getEnvironment().get(
                Context.PROVIDER_URL);
        NamingEnumeration<SearchResult> children = ctx.search(dn,
                "(objectClass=*)", scts);
        try {
            while (children.hasMore()) {
                SearchResult child = children.next();
                String subDn = child.getName();
                if (!USE_EXTERNAL_TEST_LDAP_SERVER
                        && subDn.endsWith(providerUrl)) {
                    subDn = subDn.substring(0,
                            subDn.length() - providerUrl.length() - 1);
                } else {
                    subDn = subDn + ',' + dn;
                }
                destroyRecursively(subDn, ctx, limit);
            }
        } catch (SizeLimitExceededException e) {
            log.warn("SizeLimitExceededException: trying again on partial results "
                    + dn);
            if (limit == -1) {
                limit = 100;
            }
            destroyRecursively(dn, ctx, limit - 1);
        }
        ctx.destroySubcontext(dn);
    }

    public static LDAPDirectory getLDAPDirectory(String name)
            throws DirectoryException {
        LDAPDirectoryFactory factory = (LDAPDirectoryFactory) Framework.getRuntime().getComponent(
                LDAPDirectoryFactory.NAME);
        return ((LDAPDirectoryProxy) factory.getDirectory(name)).getDirectory();
    }

}
