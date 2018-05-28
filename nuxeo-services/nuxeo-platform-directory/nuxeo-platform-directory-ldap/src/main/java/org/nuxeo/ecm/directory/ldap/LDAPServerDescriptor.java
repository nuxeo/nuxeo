/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.directory.ldap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.ldap.dns.DNSServiceEntry;
import org.nuxeo.ecm.directory.ldap.dns.DNSServiceResolver;
import org.nuxeo.ecm.directory.ldap.dns.DNSServiceResolverImpl;

import com.sun.jndi.ldap.LdapURL;

@XObject(value = "server")
public class LDAPServerDescriptor {

    public static final Log log = LogFactory.getLog(LDAPServerDescriptor.class);

    protected static final String LDAPS_SCHEME = "ldaps";

    protected static final String LDAP_SCHEME = "ldap";

    @XNode("@name")
    public String name;

    public String ldapUrls;

    public String bindDn;

    @XNode("connectionTimeout")
    public int connectionTimeout = 10000; // timeout after 10 seconds

    @XNode("poolingEnabled")
    public boolean poolingEnabled = true;

    @XNode("verifyServerCert")
    public boolean verifyServerCert = true;

    /**
     * @since 5.7
     */
    @XNode("retries")
    public int retries = 5;

    /**
     * @since 10.2
     */
    @XNode("poolingTimeout")
    protected int poolingTimeout = 60000;

    protected LinkedHashSet<LdapEntry> ldapEntries;

    protected boolean isDynamicServerList = false;

    protected boolean useSsl = false;

    protected final DNSServiceResolver srvResolver = DNSServiceResolverImpl.getInstance();

    public boolean isDynamicServerList() {
        return isDynamicServerList;
    }

    public String getName() {
        return name;
    }

    public String bindPassword = "";

    @XNode("bindDn")
    public void setBindDn(String bindDn) {
        if (null != bindDn && bindDn.trim().equals("")) {
            // empty bindDn means anonymous authentication
            this.bindDn = null;
        } else {
            this.bindDn = bindDn;
        }
    }

    public String getBindDn() {
        return bindDn;
    }

    @XNode("bindPassword")
    public void setBindPassword(String bindPassword) {
        if (bindPassword == null) {
            // no password means empty pasword
            this.bindPassword = "";
        } else {
            this.bindPassword = bindPassword;
        }
    }

    public String getBindPassword() {
        return bindPassword;
    }

    public String getLdapUrls() {
        if (ldapUrls != null) {
            return ldapUrls;
        }

        // Leverage JNDI support for clustered servers by concatenating
        // all the provided URLs for fail-over
        StringBuilder calculatedLdapUrls = new StringBuilder();
        for (LdapEntry entry : ldapEntries) {
            calculatedLdapUrls.append(entry);
            calculatedLdapUrls.append(' ');
        }

        /*
         * If the configuration does not contain any domain entries then cache the urls, domain entries should always be
         * re-queried however as the LDAP server list should change dynamically
         */
        if (!isDynamicServerList) {
            return ldapUrls = calculatedLdapUrls.toString().trim();
        }
        return calculatedLdapUrls.toString().trim();
    }

    @XNodeList(value = "ldapUrl", componentType = LDAPUrlDescriptor.class, type = LDAPUrlDescriptor[].class)
    public void setLdapUrls(LDAPUrlDescriptor[] ldapUrls) throws DirectoryException {
        if (ldapUrls == null) {
            throw new DirectoryException("At least one <ldapUrl/> server declaration is required");
        }
        ldapEntries = new LinkedHashSet<>();

        Set<LDAPUrlDescriptor> processed = new HashSet<>();

        List<String> urls = new ArrayList<>(ldapUrls.length);
        for (LDAPUrlDescriptor url : ldapUrls) {
            LdapURL ldapUrl;
            try {
                /*
                 * Empty string translates to ldap://localhost:389 through JNDI
                 */
                if (StringUtils.isEmpty(url.getValue())) {
                    urls.add(url.getValue());
                    ldapEntries.add(new LdapEntryDescriptor(url));
                    continue;
                }

                /*
                 * Parse the URI to make sure it is valid
                 */
                ldapUrl = new LdapURL(url.getValue());
                if (!processed.add(url)) {
                    continue;
                }
            } catch (NamingException e) {
                throw new DirectoryException(e);
            }

            useSsl = useSsl || ldapUrl.useSsl();

            /*
             * RFC-2255 - The "ldap" prefix indicates an entry or entries residing in the LDAP server running on the
             * given hostname at the given port number. The default LDAP port is TCP port 389. If no hostport is given,
             * the client must have some apriori knowledge of an appropriate LDAP server to contact.
             */
            if (ldapUrl.getHost() == null) {
                /*
                 * RFC-2782 - Check to see if an LDAP SRV record is defined in the DNS server
                 */
                String domain = convertDNtoFQDN(ldapUrl.getDN());
                if (domain != null) {
                    /*
                     * Dynamic URL - retrieve from SRV record
                     */
                    List<String> discoveredUrls;
                    try {
                        discoveredUrls = discoverLdapServers(domain, ldapUrl.useSsl(), url.getSrvPrefix());
                    } catch (NamingException e) {
                        throw new DirectoryException(String.format("SRV record DNS lookup failed for %s.%s: %s",
                                url.getSrvPrefix(), domain, e.getMessage()), e);
                    }

                    /*
                     * Discovered URLs could be empty, lets check at the end though
                     */
                    urls.addAll(discoveredUrls);

                    /*
                     * Store entries in an ordered set and remember that we were dynamic
                     */
                    ldapEntries.add(new LdapEntryDomain(url, domain, ldapUrl.useSsl()));
                    isDynamicServerList = true;
                } else {
                    throw new DirectoryException(
                            "Invalid LDAP SRV reference, this should be of the form" + " ldap:///dc=example,dc=org");
                }
            } else {
                /*
                 * Static URL - store the value
                 */
                urls.add(url.getValue());

                /*
                 * Store entries in an ordered set
                 */
                ldapEntries.add(new LdapEntryDescriptor(url));
            }
        }

        /*
         * Oops no valid URLs to connect to :(
         */
        if (urls.isEmpty()) {
            throw new DirectoryException("No valid server urls returned from DNS query");
        }
    }

    /**
     * Whether this server descriptor defines a secure ldap connection
     */
    public boolean useSsl() {
        return useSsl;
    }

    /**
     * Retrieve server URLs from DNS SRV record
     *
     * @param domain The domain to query
     * @param useSsl Whether the connection to this domain should be secure
     * @return List of servers or empty list
     * @throws NamingException if DNS lookup fails
     */
    protected List<String> discoverLdapServers(String domain, boolean useSsl, String srvPrefix) throws NamingException {
        List<String> result = new ArrayList<>();
        List<DNSServiceEntry> servers = getSRVResolver().resolveLDAPDomainServers(domain, srvPrefix);

        for (DNSServiceEntry serviceEntry : servers) {
            /*
             * Rebuild the URL
             */
            StringBuilder realUrl = (useSsl) ? new StringBuilder(LDAPS_SCHEME + "://")
                    : new StringBuilder(LDAP_SCHEME + "://");
            realUrl.append(serviceEntry);
            result.add(realUrl.toString());
        }
        return result;
    }

    /**
     * Convert domain from the ldap form dc=nuxeo,dc=org to the DNS domain name form nuxeo.org
     *
     * @param dn base DN of the domain
     * @return the FQDN or null is DN is not matching the expected structure
     * @throws DirectoryException is the DN is invalid
     */
    protected String convertDNtoFQDN(String dn) throws DirectoryException {
        try {
            LdapDN ldapDN = new LdapDN(dn);
            Enumeration<String> components = ldapDN.getAll();
            List<String> domainComponents = new ArrayList<>();
            while (components.hasMoreElements()) {
                String component = components.nextElement();
                if (component.startsWith("dc=")) {
                    domainComponents.add(component.substring(3));
                } else {
                    break;
                }
            }
            Collections.reverse(domainComponents);
            return StringUtils.join(domainComponents, ".");
        } catch (InvalidNameException e) {
            throw new DirectoryException(e);
        }
    }

    public boolean isPoolingEnabled() {
        return poolingEnabled;
    }

    public boolean isVerifyServerCert() {
        return verifyServerCert;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * @since 10.2
     */
    public int getPoolingTimeout() {
        return poolingTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    protected DNSServiceResolver getSRVResolver() {
        return srvResolver;
    }

    /**
     * Common internal interface for Ldap entries
     *
     * @author Bob Browning
     */
    protected interface LdapEntry {
        String getUrl() throws NamingException;
    }

    /**
     * Server URL implementation of {@link LdapEntry}
     *
     * @author Bob Browning
     */
    protected class LdapEntryDescriptor implements LdapEntry {

        protected LDAPUrlDescriptor url;

        public LdapEntryDescriptor(LDAPUrlDescriptor descriptor) {
            url = descriptor;
        }

        @Override
        public String toString() {
            try {
                return getUrl();
            } catch (NamingException e) {
                log.error(e, e);
                return "[DNS lookup failed]";
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof LdapEntryDescriptor) {
                return url.equals(((LdapEntryDescriptor) obj).url);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return url.hashCode();
        }

        @Override
        public String getUrl() throws NamingException {
            return url.getValue();
        }

    }

    /**
     * Domain implementation of {@link LdapEntry} using DNS SRV record
     *
     * @author Bob Browning
     */
    protected final class LdapEntryDomain extends LdapEntryDescriptor {

        protected final String domain;

        protected final boolean useSsl;

        public LdapEntryDomain(LDAPUrlDescriptor descriptor, final String domain, boolean useSsl) {
            super(descriptor);
            this.domain = domain;
            this.useSsl = useSsl;
        }

        @Override
        public String getUrl() throws NamingException {
            List<DNSServiceEntry> servers = getSRVResolver().resolveLDAPDomainServers(domain, url.getSrvPrefix());

            StringBuilder result = new StringBuilder();
            for (DNSServiceEntry serviceEntry : servers) {
                /*
                 * Rebuild the URL
                 */
                result.append(useSsl ? LDAPS_SCHEME + "://" : LDAP_SCHEME + "://");
                result.append(serviceEntry);
                result.append(' ');
            }
            return result.toString().trim();
        }

        private LDAPServerDescriptor getOuterType() {
            return LDAPServerDescriptor.this;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((domain == null) ? 0 : domain.hashCode());
            result = prime * result + (useSsl ? 1231 : 1237);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj)) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            LdapEntryDomain other = (LdapEntryDomain) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (domain == null) {
                if (other.domain != null) {
                    return false;
                }
            } else if (!domain.equals(other.domain)) {
                return false;
            }
            if (useSsl != other.useSsl) {
                return false;
            }
            return true;
        }
    }

    /**
     * @since 5.7
     */
    public int getRetries() {
        return retries;
    }

}
