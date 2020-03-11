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
 *     Nuxeo - code review and integration
 */
package org.nuxeo.ecm.directory.ldap.dns;

import java.util.List;

import javax.naming.NamingException;

/**
 * Utility to fetch SRV records from a DNS server to get the list of available ldap servers from the DN representation
 * of the domain.
 * <p>
 * See: http://en.wikipedia.org/wiki/SRV_record
 *
 * @author Robert Browning
 */
public interface DNSServiceResolver {

    /**
     * DNS Cache Expiry property
     */
    String DNS_CACHE_EXPIRY = "org.nuxeo.ecm.directory.ldap.dns.cache.expiry";

    /**
     * Prefix to locate LDAP service on DNS Server.
     * <p>
     * <b>service</b>: _ldap<br/>
     * <b>protocol</b>: _tcp
     */
    String LDAP_SERVICE_PREFIX = "_ldap._tcp";

    /**
     * Returns a list of LDAP servers for the specified domain by performing an SRV DNS lookup on _ldap._tcp.${domain}.
     *
     * @param domain
     * @return the list of SRV dns entries
     * @throws NamingException
     */
    List<DNSServiceEntry> resolveLDAPDomainServers(final String domain) throws NamingException;

    /**
     * Returns a list of LDAP servers for the specified domain by performing an SRV DNS lookup using a custom DNS
     * service prefix.
     *
     * @param domain
     * @param prefix custom SRV prefix such as "_gc._tcp"
     * @return the list of SRV dns entries
     * @throws NamingException
     */
    List<DNSServiceEntry> resolveLDAPDomainServers(final String domain, final String prefix) throws NamingException;

}
