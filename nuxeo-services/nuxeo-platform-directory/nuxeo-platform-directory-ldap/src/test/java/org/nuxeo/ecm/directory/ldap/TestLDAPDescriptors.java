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

import java.net.URL;
import java.util.Map;

import javax.naming.directory.SearchControls;

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestLDAPDescriptors extends NXRuntimeTestCase {

    protected LDAPDirectoryDescriptor directory;

    protected LDAPServerDescriptor server1;

    protected LDAPServerDescriptor server2;

    public static URL getResource(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(
                resource);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        XMap xmap = new XMap();
        xmap.register(LDAPServerDescriptor.class);
        xmap.register(LDAPDirectoryDescriptor.class);

        URL directoryUrl = getResource("directory.xml");
        directory = (LDAPDirectoryDescriptor) xmap.load(directoryUrl);

        URL server1Url = getResource("server1.xml");
        server1 = (LDAPServerDescriptor) xmap.load(server1Url);

        URL server2Url = getResource("server2.xml");
        server2 = (LDAPServerDescriptor) xmap.load(server2Url);
    }

    public void testGetRdnAttribute() {
        assertEquals("uid", directory.getRdnAttribute());
    }

    public void testGetCreationBaseDn() {
        assertEquals("ou=people,dc=example,dc=com",
                directory.getCreationBaseDn());
    }

    public void testGetCreationClasses() {
        String[] configuredClasses = directory.getCreationClasses();
        assertEquals(4, configuredClasses.length);
        assertEquals("top", configuredClasses[0]);
        assertEquals("person", configuredClasses[1]);
        assertEquals("organizationalPerson", configuredClasses[2]);
        assertEquals("inetOrgPerson", configuredClasses[3]);
    }

    public void testGetIdField() {
        assertEquals("uid", directory.getIdField());
    }

    public void testGetPasswordFieldName() {
        assertEquals("userPassword", directory.getPasswordField());
    }

    public void testGetSchemaName() {
        assertEquals("user", directory.getSchemaName());
    }

    public void testGetSearchBaseDn() {
        assertEquals("ou=people,dc=example,dc=com",
                directory.getSearchBaseDn());
    }

    public void testGetSearchClasses() {
        // test data from the directory.xml resource
        String[] configuredClasses = directory.getSearchClasses();
        assertEquals(1, configuredClasses.length);
        assertEquals("person", configuredClasses[0]);
    }

    public void testGetSearchFilter() {
        assertEquals("(&(sn=Aa*)(cn=Aa*))", directory.getSearchFilter());
    }

    public void testGetAggregatedSearchFilter() {
        // test aggregation based on data from the directory.xml
        // resource
        assertEquals("(&(objectClass=person)(&(sn=Aa*)(cn=Aa*)))",
                directory.getAggregatedSearchFilter());

        // empty filter
        directory.setSearchClasses(null);
        directory.searchFilter = null;
        assertEquals("(objectClass=*)", directory.getAggregatedSearchFilter());

        // several search classes and no search filter
        String[] twoClasses = {"person", "organizationalUnit"};
        directory.setSearchClasses(twoClasses);
        directory.searchFilter = null;
        assertEquals("(|(objectClass=person)(objectClass=organizationalUnit))",
                directory.getAggregatedSearchFilter());

        // several search classes and a search filter
        directory.setSearchClasses(twoClasses);
        directory.searchFilter = "(&(sn=Aa*)(cn=Aa*))";
        assertEquals("(&(|(objectClass=person)(objectClass=organizationalUnit))"
                + "(&(sn=Aa*)(cn=Aa*)))",
                directory.getAggregatedSearchFilter());
    }

    public void testGetSearchScope() throws DirectoryException {
        // testing the value provided in the directory.xml resource
        assertEquals(SearchControls.ONELEVEL_SCOPE,
                directory.getSearchScope());

        // testing funky but valid values
        directory.setSearchScope("SUbTrEe");
        assertEquals(SearchControls.SUBTREE_SCOPE, directory.getSearchScope());
        directory.setSearchScope("OBJECT");
        assertEquals(SearchControls.OBJECT_SCOPE, directory.getSearchScope());

        // default value
        directory.setSearchScope(null);
        assertEquals(SearchControls.ONELEVEL_SCOPE, directory.getSearchScope());

        // testing bad scope
        try {
            directory.setSearchScope("this is a bad bad scope");
            fail("Should have raised an DirectoryException");
        } catch (DirectoryException e) {
        }
    }

    public void testGetName() {
        assertEquals("directoryName", directory.getName());
        assertEquals("server1Name", server1.getName());
        assertEquals("server2Name", server2.getName());
    }

    public void testGetServerName() {
        assertEquals("default", directory.getServerName());
    }

    public void testMapper() {
        Map<String, String> fieldMapping = directory.getFieldMapping();
        assertNotNull(fieldMapping);
        assertTrue(fieldMapping.containsKey("firstName"));
        assertTrue(fieldMapping.containsKey("lastName"));
        assertTrue(fieldMapping.containsKey("company"));
    }

    public void testGetLdapUrls() {
        assertEquals("ldap://localhost", server1.getLdapUrls());
        assertEquals("ldap://localhost:389 ldap://server2 ldap://server3",
                server2.getLdapUrls());
        // test required attribute
        try {
            server1.setLdapUrls(null);
            fail("Should have raised an DirectoryException");
        } catch (DirectoryException e) {
        }
    }

    public void testGetBindDn() {
        assertNull(server1.getBindDn());
        assertEquals("cn=nuxeo5,ou=applications,dc=example,dc=com",
                server2.getBindDn());
    }

    public void testGetBindPassword() {
        assertEquals("", server1.getBindPassword());
        assertEquals("changeme", server2.getBindPassword());
    }

    public void testIsPoolingEnabled() {
        assertTrue(server1.isPoolingEnabled());
        assertFalse(server2.isPoolingEnabled());
    }

    public void testGetEmptyRefMarker() {
        assertEquals("cn=emptyRef", directory.getEmptyRefMarker());
    }

}
