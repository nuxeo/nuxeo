/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Robert Browning - initial implementation
 */
package org.nuxeo.ecm.directory.ldap.dns;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.directory.ldap.LDAPServerDescriptor;
import org.nuxeo.ecm.directory.ldap.LDAPUrlDescriptor;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.sun.jndi.ldap.LdapURL;

/**
 * Test case to ensure LDAPServerDescriptor correctly handles entries returned from DNSService implementation
 *
 * @author Bob Browning
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class LDAPServerDescriptorDNSTestCase {

    private static final Log log = LogFactory.getLog(LDAPServerDescriptorDNSTestCase.class);

    private final class MockDNSService implements DNSServiceResolver {

        @Override
        public List<DNSServiceEntry> resolveLDAPDomainServers(String domain, String prefix) {
            List<DNSServiceEntry> entries = new ArrayList<>();
            if (prefix.equals("_gc._tcp")) {
                entries.add(new DNSServiceEntry("localhost", 3268, 0, 100));
            } else if (prefix.equals("_ldap._tcp")) {
                entries.add(new DNSServiceEntry("localhost", 389, 0, 100));
            }
            return entries;
        }

        @Override
        public List<DNSServiceEntry> resolveLDAPDomainServers(String domain) {
            List<DNSServiceEntry> entries = new ArrayList<>();
            entries.add(new DNSServiceEntry("localhost", 389, 0, 100));
            return entries;
        }

    }

    private final class MockLDAPServerDescriptor extends LDAPServerDescriptor {
        /*
         * Mock DNS Service
         */
        private final DNSServiceResolver service = new MockDNSService();

        @Override
        protected DNSServiceResolver getSRVResolver() {
            return service;
        }

    }

    private MockDNSService dns;

    @Before
    public void setUp() throws Exception {
        dns = new MockDNSService();
    }

    private final String domain = "ldap:///dc=nuxeo,dc=org";

    /**
     * Unit test the set/get LdapUrl methods
     *
     * @throws Exception
     */
    @Test
    public void testLdapServerDnsParsing() throws Exception {
        List<DNSServiceEntry> actual = dns.resolveLDAPDomainServers("nuxeo.org");
        if (log.isDebugEnabled()) {
            log.debug(actual);
        }

        /*
         * Convert our discovered server list into URIs
         */
        LDAPUrlDescriptor[] uris = new LDAPUrlDescriptor[actual.size()];
        int i = 0;
        for (DNSServiceEntry serviceEntry : actual) {
            LDAPUrlDescriptor u = new LDAPUrlDescriptor();
            u.setValue("ldap://" + serviceEntry.toString());
            uris[i++] = u;
        }

        LDAPServerDescriptor d = new MockLDAPServerDescriptor();

        d.setLdapUrls(uris);
        String testA = d.getLdapUrls();
        if (log.isDebugEnabled()) {
            log.debug(testA);
        }
        assertEquals("ldap://localhost:389", testA);

        d = new MockLDAPServerDescriptor();

        /*
         * _ldap._tcp test
         */
        LDAPUrlDescriptor u = new LDAPUrlDescriptor();
        u.setValue(domain);

        /*
         * _gc._tcp test
         */
        LDAPUrlDescriptor u2 = new LDAPUrlDescriptor();
        u2.setSrvPrefix("_gc._tcp");
        u2.setValue(domain);

        d.setLdapUrls(new LDAPUrlDescriptor[] { u, u2 });

        for (int j = 0; j < 100; j++) {
            String urls = d.getLdapUrls();
            if (log.isDebugEnabled()) {
                log.debug(urls);
            }
            assertEquals("ldap://localhost:389 ldap://localhost:3268", urls);
        }

        d.setLdapUrls(new LDAPUrlDescriptor[] { u });

        /*
         * Assert that when run using empty hostname in URI we get the same result as passing in the list of servers
         */
        assertEquals(testA, d.getLdapUrls());
    }

    /**
     * Ensure LdapURL correctly determines SSL support of server URL
     *
     * @throws Exception
     */
    @Test
    public void testLdapUrlSslSupport() throws Exception {
        LdapURL url = new LdapURL("ldap:///dc=example,dc=com");
        assertFalse(url.useSsl());

        url = new LdapURL("ldaps:///dc=example,dc=com");
        assertTrue(url.useSsl());
    }
}
