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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.Map;

import javax.naming.directory.SearchControls;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestLDAPDescriptors {

    protected LDAPDirectoryDescriptor descriptor;

    protected LDAPServerDescriptor server1;

    protected LDAPServerDescriptor server2;

    public static URL getResource(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(resource);
    }

    @Before
    public void setUp() throws Exception {
        XMap xmap = new XMap();
        xmap.register(LDAPServerDescriptor.class);
        xmap.register(LDAPDirectoryDescriptor.class);

        URL directoryUrl = getResource("directory.xml");
        descriptor = (LDAPDirectoryDescriptor) xmap.load(directoryUrl);

        URL server1Url = getResource("server1.xml");
        server1 = (LDAPServerDescriptor) xmap.load(server1Url);

        URL server2Url = getResource("server2.xml");
        server2 = (LDAPServerDescriptor) xmap.load(server2Url);
    }

    @Test
    public void testGetRdnAttribute() {
        assertEquals("uid", descriptor.getRdnAttribute());
    }

    @Test
    public void testGetCreationBaseDn() {
        assertEquals("ou=people,dc=example,dc=com", descriptor.getCreationBaseDn());
    }

    @Test
    public void testGetCreationClasses() {
        String[] configuredClasses = descriptor.getCreationClasses();
        assertEquals(4, configuredClasses.length);
        assertEquals("top", configuredClasses[0]);
        assertEquals("person", configuredClasses[1]);
        assertEquals("organizationalPerson", configuredClasses[2]);
        assertEquals("inetOrgPerson", configuredClasses[3]);
    }

    @Test
    public void testGetIdField() {
        assertEquals("uid", descriptor.idField);
    }

    @Test
    public void testGetPasswordFieldName() {
        assertEquals("userPassword", descriptor.passwordField);
    }

    @Test
    public void testGetSchemaName() {
        assertEquals("user", descriptor.schemaName);
    }

    @Test
    public void testGetSearchBaseDn() {
        assertEquals("ou=people,dc=example,dc=com", descriptor.getSearchBaseDn());
    }

    @Test
    public void testGetSearchClasses() {
        // test data from the directory.xml resource
        String[] configuredClasses = descriptor.getSearchClasses();
        assertEquals(1, configuredClasses.length);
        assertEquals("person", configuredClasses[0]);
    }

    @Test
    public void testGetSearchFilter() {
        assertEquals("(&(sn=Aa*)(cn=Aa*))", descriptor.getSearchFilter());
    }

    @Test
    public void testGetAggregatedSearchFilter() {
        // test aggregation based on data from the directory.xml
        // resource
        assertEquals("(&(objectClass=person)(&(sn=Aa*)(cn=Aa*)))", descriptor.getAggregatedSearchFilter());

        // empty filter
        descriptor.setSearchClasses(null);
        descriptor.searchFilter = null;
        assertEquals("(objectClass=*)", descriptor.getAggregatedSearchFilter());

        // several search classes and no search filter
        String[] twoClasses = { "person", "organizationalUnit" };
        descriptor.setSearchClasses(twoClasses);
        descriptor.searchFilter = null;
        assertEquals("(|(objectClass=person)(objectClass=organizationalUnit))", descriptor.getAggregatedSearchFilter());

        // several search classes and a search filter
        descriptor.setSearchClasses(twoClasses);
        descriptor.searchFilter = "(&(sn=Aa*)(cn=Aa*))";
        assertEquals("(&(|(objectClass=person)(objectClass=organizationalUnit))" + "(&(sn=Aa*)(cn=Aa*)))",
                descriptor.getAggregatedSearchFilter());
    }

    @Test
    public void testGetSearchScope() throws DirectoryException {
        // testing the value provided in the directory.xml resource
        assertEquals(SearchControls.ONELEVEL_SCOPE, descriptor.getSearchScope());

        // testing funky but valid values
        descriptor.setSearchScope("SUbTrEe");
        assertEquals(SearchControls.SUBTREE_SCOPE, descriptor.getSearchScope());
        descriptor.setSearchScope("OBJECT");
        assertEquals(SearchControls.OBJECT_SCOPE, descriptor.getSearchScope());

        // default value
        descriptor.setSearchScope(null);
        assertEquals(SearchControls.ONELEVEL_SCOPE, descriptor.getSearchScope());

        // testing bad scope
        try {
            descriptor.setSearchScope("this is a bad bad scope");
            fail("Should have raised an DirectoryException");
        } catch (DirectoryException e) {
        }
    }

    @Test
    public void testGetName() {
        assertEquals("directoryName", descriptor.name);
        assertEquals("server1Name", server1.getName());
        assertEquals("server2Name", server2.getName());
    }

    @Test
    public void testGetServerName() {
        assertEquals("default", descriptor.getServerName());
    }

    @Test
    public void testMapper() {
        Map<String, String> fieldMapping = descriptor.getFieldMapping();
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
        assertEquals("cn=emptyRef", descriptor.getEmptyRefMarker());
    }

    /**
     * @since 10.2
     */
    @Test
    public void testGetPoolingTimeout() {
        assertEquals(60000, server1.getPoolingTimeout());
        assertEquals(300000, server2.getPoolingTimeout());
    }

}
