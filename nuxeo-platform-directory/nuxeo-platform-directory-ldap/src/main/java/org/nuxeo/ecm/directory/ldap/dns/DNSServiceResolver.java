/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Robert Browning - initial implementation
 *     Nuxeo - code review and integration
 */
package org.nuxeo.ecm.directory.ldap.dns;

import java.util.List;

import javax.naming.NamingException;

/**
 * Utility to fetch SRV records from a DNS server to get the list of available
 * ldap servers from the DN representation of the domain.
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
     * Returns a list of LDAP servers for the specified domain by performing an
     * SRV DNS lookup on _ldap._tcp.${domain}.
     *
     * @param domain
     * @return the list of SRV dns entries
     * @throws NamingException
     */
    List<DNSServiceEntry> resolveLDAPDomainServers(final String domain)
            throws NamingException;

    /**
     * Returns a list of LDAP servers for the specified domain by performing an
     * SRV DNS lookup using a custom DNS service prefix.
     *
     * @param domain
     * @param prefix custom SRV prefix such as "_gc._tcp"
     * @return the list of SRV dns entries
     * @throws NamingException
     */
    List<DNSServiceEntry> resolveLDAPDomainServers(final String domain,
            final String prefix) throws NamingException;

}
