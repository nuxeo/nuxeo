/*
 * (C) Copyright 2010-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - code review and integration
 */
package org.nuxeo.ecm.directory.ldap.dns;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * Utility class to perform DNS lookups for services.
 */
public class DNSServiceResolverImpl implements DNSServiceResolver {

    public static final Log log = LogFactory.getLog(DNSServiceResolverImpl.class);

    protected static DNSServiceResolver instance;

    protected static final String SRV_RECORD = "SRV";

    /**
     * Create a cache to hold the at most 100 recent DNS lookups for a period of 10 minutes.
     */
    protected Map<String, List<DNSServiceEntry>> cache = new HashMap<>();

    protected long lastCacheUpdate = System.currentTimeMillis();

    protected final long maxDelay;

    protected final DirContext context;

    public static synchronized DNSServiceResolver getInstance() {
        if (instance == null) {
            instance = new DNSServiceResolverImpl();
        }
        return instance;
    }

    protected DNSServiceResolverImpl() {
        /*
         * The expiry of the cache in minutes
         */
        int cacheExpiry = 10;
        try {
            cacheExpiry = Integer.parseInt(Framework.getProperty(DNS_CACHE_EXPIRY, "10"));
        } catch (NumberFormatException e) {
            log.warn("invalid value for property: " + DNS_CACHE_EXPIRY
                    + ", falling back to default value of 10 minutes");
        }
        maxDelay = Duration.ofMinutes(cacheExpiry).toMillis();

        Properties env = new Properties();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        try {
            context = new InitialDirContext(env);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the host name and port that a server providing the specified service can be reached at. A DNS lookup for
     * a SRV record in the form "_service.example.com" is attempted.
     * <p>
     * As an example, a lookup for "example.com" for the service _gc._tcp may return "dc01.example.com:3268".
     *
     * @param service the service.
     * @param domain the domain.
     * @return a List of DNSServiceEntrys, which encompasses the hostname and port that the server can be reached at for
     *         the specified domain.
     * @throws NamingException if the DNS server is unreachable
     */
    protected List<DNSServiceEntry> resolveDnsServiceRecord(final String service, final String domain)
            throws NamingException {
        List<DNSServiceEntry> addresses = new ArrayList<>();

        if (context == null) {
            return addresses;
        }

        final String key = service + "." + domain;
        /*
         * Return item from cache if it exists.
         */
        if (System.currentTimeMillis() - lastCacheUpdate > maxDelay) {
            cache.clear();
        }
        if (cache.containsKey(key)) {
            List<DNSServiceEntry> cachedAddresses = cache.get(key);
            if (cachedAddresses != null) {
                return cachedAddresses;
            }
        }

        Attributes dnsLookup = context.getAttributes(service + "." + domain, new String[] { SRV_RECORD });

        Attribute attribute = dnsLookup.get(SRV_RECORD);
        for (int i = 0; i < attribute.size(); i++) {
            /*
             * Get the current resource record
             */
            String entry = (String) attribute.get(i);

            String[] records = entry.split(" ");
            String host = records[records.length - 1];
            int port = Integer.parseInt(records[records.length - 2]);
            int weight = Integer.parseInt(records[records.length - 3]);
            int priority = Integer.parseInt(records[records.length - 4]);

            /*
             * possible to get TTL?
             */

            /*
             * Host entries in DNS should end with a "."
             */
            if (host.endsWith(".")) {
                host = host.substring(0, host.length() - 1);
            }

            addresses.add(new DNSServiceEntry(host, port, priority, weight));
        }

        /*
         * Sort the addresses by DNS priority and weight settings
         */
        Collections.sort(addresses);

        /*
         * Add item to cache.
         */
        if (cache.size() > 100) {
            cache.clear();
        }
        cache.put(key, addresses);
        lastCacheUpdate = System.currentTimeMillis();
        return addresses;
    }

    @Override
    public List<DNSServiceEntry> resolveLDAPDomainServers(final String domain) throws NamingException {
        return resolveDnsServiceRecord(LDAP_SERVICE_PREFIX, domain);
    }

    @Override
    public List<DNSServiceEntry> resolveLDAPDomainServers(final String domain, final String prefix)
            throws NamingException {
        return resolveDnsServiceRecord(prefix, domain);
    }

}
