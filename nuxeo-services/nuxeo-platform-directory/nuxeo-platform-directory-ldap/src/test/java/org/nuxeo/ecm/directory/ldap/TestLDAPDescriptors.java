/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.directory.ldap;

import java.net.URL;
import java.util.Map;

import javax.naming.directory.SearchControls;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestLDAPDescriptors extends NXRuntimeTestCase {

    protected LDAPDirectoryDescriptor directory;

    protected LDAPServerDescriptor server1;

    protected LDAPServerDescriptor server2;

    public static URL getResource(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(resource);
    }

    @Override
    @Test
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

    @Test
    public void testGetRdnAttribute() {
        assertEquals("uid", directory.getRdnAttribute());
    }

    @Test
    public void testGetCreationBaseDn() {
        assertEquals("ou=people,dc=example,dc=com", directory.getCreationBaseDn());
    }

    @Test
    public void testGetCreationClasses() {
        String[] configuredClasses = directory.getCreationClasses();
        assertEquals(4, configuredClasses.length);
        assertEquals("top", configuredClasses[0]);
        assertEquals("person", configuredClasses[1]);
        assertEquals("organizationalPerson", configuredClasses[2]);
        assertEquals("inetOrgPerson", configuredClasses[3]);
    }

    @Test
    public void testGetIdField() {
        assertEquals("uid", directory.getIdField());
    }

    @Test
    public void testGetPasswordFieldName() {
        assertEquals("userPassword", directory.getPasswordField());
    }

    @Test
    public void testGetSchemaName() {
        assertEquals("user", directory.getSchemaName());
    }

    @Test
    public void testGetSearchBaseDn() {
        assertEquals("ou=people,dc=example,dc=com", directory.getSearchBaseDn());
    }

    @Test
    public void testGetSearchClasses() {
        // test data from the directory.xml resource
        String[] configuredClasses = directory.getSearchClasses();
        assertEquals(1, configuredClasses.length);
        assertEquals("person", configuredClasses[0]);
    }

    @Test
    public void testGetSearchFilter() {
        assertEquals("(&(sn=Aa*)(cn=Aa*))", directory.getSearchFilter());
    }

    @Test
    public void testGetAggregatedSearchFilter() {
        // test aggregation based on data from the directory.xml
        // resource
        assertEquals("(&(objectClass=person)(&(sn=Aa*)(cn=Aa*)))", directory.getAggregatedSearchFilter());

        // empty filter
        directory.setSearchClasses(null);
        directory.searchFilter = null;
        assertEquals("(objectClass=*)", directory.getAggregatedSearchFilter());

        // several search classes and no search filter
        String[] twoClasses = { "person", "organizationalUnit" };
        directory.setSearchClasses(twoClasses);
        directory.searchFilter = null;
        assertEquals("(|(objectClass=person)(objectClass=organizationalUnit))", directory.getAggregatedSearchFilter());

        // several search classes and a search filter
        directory.setSearchClasses(twoClasses);
        directory.searchFilter = "(&(sn=Aa*)(cn=Aa*))";
        assertEquals("(&(|(objectClass=person)(objectClass=organizationalUnit))" + "(&(sn=Aa*)(cn=Aa*)))",
                directory.getAggregatedSearchFilter());
    }

    @Test
    public void testGetSearchScope() throws DirectoryException {
        // testing the value provided in the directory.xml resource
        assertEquals(SearchControls.ONELEVEL_SCOPE, directory.getSearchScope());

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

    @Test
    public void testGetName() {
        assertEquals("directoryName", directory.getName());
        assertEquals("server1Name", server1.getName());
        assertEquals("server2Name", server2.getName());
    }

    @Test
    public void testGetServerName() {
        assertEquals("default", directory.getServerName());
    }

    @Test
    public void testMapper() {
        Map<String, String> fieldMapping = directory.getFieldMapping();
        assertNotNull(fieldMapping);
        assertTrue(fieldMapping.containsKey("firstName"));
        assertTrue(fieldMapping.containsKey("lastName"));
        assertTrue(fieldMapping.containsKey("company"));
    }

    @Test
    public void testGetLdapUrls() {
        assertEquals("ldap://localhost", server1.getLdapUrls());
        assertEquals("ldap://localhost:389 ldap://server2 ldap://server3", server2.getLdapUrls());
        // test required attribute
        try {
            server1.setLdapUrls(null);
            fail("Should have raised an DirectoryException");
        } catch (DirectoryException e) {
        }
    }

    @Test
    public void testGetBindDn() {
        assertNull(server1.getBindDn());
        assertEquals("cn=nuxeo5,ou=applications,dc=example,dc=com", server2.getBindDn());
    }

    @Test
    public void testGetBindPassword() {
        assertEquals("", server1.getBindPassword());
        assertEquals("changeme", server2.getBindPassword());
    }

    @Test
    public void testIsPoolingEnabled() {
        assertTrue(server1.isPoolingEnabled());
        assertFalse(server2.isPoolingEnabled());
    }

    @Test
    public void testIsVerifyServerCert() {
        assertTrue(server1.isVerifyServerCert());
        assertFalse(server2.isVerifyServerCert());
    }

    @Test
    public void testGetEmptyRefMarker() {
        assertEquals("cn=emptyRef", directory.getEmptyRefMarker());
    }

}
