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
 * $Id$
 */

package org.nuxeo.ecm.directory.ldap;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.AbstractDirectory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.DirectoryFieldMapper;
import org.nuxeo.ecm.directory.Reference;
import org.nuxeo.ecm.directory.SecuredSession;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation of the Directory interface for servers implementing the
 * Lightweight Directory Access Protocol.
 *
 * @author ogrisel
 * @author Robert Browning
 */
public class LDAPDirectory extends AbstractDirectory {

    private static final Log log = LogFactory.getLog(LDAPDirectory.class);

    // special field key to be able to read the DN of an LDAP entry
    public static final String DN_SPECIAL_ATTRIBUTE_KEY = "dn";

    protected final LDAPDirectoryDescriptor config;

    protected Properties contextProperties;

    protected SearchControls searchControls;

    protected Map<String, Field> schemaFieldMap;

    protected final LDAPDirectoryFactory factory;

    protected String baseFilter;

    // the following attribute is only used for testing purpose
    protected ContextProvider testServer;

    public LDAPDirectory(LDAPDirectoryDescriptor config) throws ClientException {
        super(config.name);
        this.config = config;
        factory = (LDAPDirectoryFactory) Framework.getRuntime().getComponent(
                LDAPDirectoryFactory.NAME);

        if (config.getIdField() == null || config.getIdField().equals("")) {
            throw new DirectoryException(
                    "idField configuration is missing for directory "
                            + config.getName());
        }
        if (config.getSchemaName() == null || config.getSchemaName().equals("")) {
            throw new DirectoryException(
                    "schema configuration is missing for directory "
                            + config.getName());
        }
        if (config.getSearchBaseDn() == null
                || config.getSearchBaseDn().equals("")) {
            throw new DirectoryException(
                    "searchBaseDn configuration is missing for directory "
                            + config.getName());
        }

    }

    @Override
    public Reference getReference(String referenceFieldName) {
        if(schemaFieldMap == null)
        {
            initLDAPConfig();
        }
        return references.get(referenceFieldName);
    }

    /**
     * Lazy init method for ldap config
     *
     * @since 5.9.6
     */
    protected void initLDAPConfig()
    {
        // computing attributes that will be useful for all sessions
        SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
        Schema schema = schemaManager.getSchema(config.getSchemaName());
        if (schema == null) {
            throw new DirectoryException(config.getSchemaName()
                    + " is not a registered schema");
        }
        schemaFieldMap = new LinkedHashMap<String, Field>();
        for (Field f : schema.getFields()) {
            schemaFieldMap.put(f.getName().getLocalName(), f);
        }

        // init field mapper before search fields
        fieldMapper = new DirectoryFieldMapper(config.fieldMapping);
        contextProperties = computeContextProperties();
        baseFilter = config.getAggregatedSearchFilter();

        // register the references
        addReferences(config.getInverseReferences());
        addReferences(config.getLdapReferences());

        // register the search controls after having registered the references
        // since the list of attributes to fetch my depend on registered
        // LDAPReferences
        searchControls = computeSearchControls();

        // cache parameterization
        cache.setEntryCacheName(config.cacheEntryName);
        cache.setEntryCacheWithoutReferencesName(config.cacheEntryWithoutReferencesName);

        log.debug(String.format(
                "initialized LDAP directory %s with fields [%s] and references [%s]",
                config.getName(),
                StringUtils.join(schemaFieldMap.keySet().toArray(), ", "),
                StringUtils.join(references.keySet().toArray(), ", ")));
    }

    /**
     * @return connection parameters to use for all LDAP queries
     */
    protected Properties computeContextProperties() throws DirectoryException {
        // Initialization of LDAP connection parameters from parameters
        // registered in the LDAP "server" extension point
        Properties props = new Properties();
        LDAPServerDescriptor serverConfig = getServer();

        if (null == serverConfig) {
            throw new DirectoryException(
                    "LDAP server configuration not found: "
                            + config.getServerName());
        }

        props.put(Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.ldap.LdapCtxFactory");

        /*
         * Get initial connection URLs, dynamic URLs may cause the list to be
         * updated when creating the session
         */
        String ldapUrls = serverConfig.getLdapUrls();
        if (serverConfig.getLdapUrls() == null
                || config.getSchemaName().equals("")) {
            throw new DirectoryException(
                    "Server LDAP URL configuration is missing for directory "
                            + config.getName());
        }
        props.put(Context.PROVIDER_URL, ldapUrls);

        // define how referrals are handled
        if (!getConfig().followReferrals) {
            props.put(Context.REFERRAL, "ignore");
        } else {
            // this is the default mode
            props.put(Context.REFERRAL, "follow");
        }

        /*
         * SSL Connections do not work with connection timeout property
         */
        if (serverConfig.getConnectionTimeout() > -1) {
            if (!serverConfig.useSsl()) {
                props.put("com.sun.jndi.ldap.connect.timeout",
                        Integer.toString(serverConfig.getConnectionTimeout()));
            } else {
                log.warn("SSL connections do not operate correctly"
                        + " when used with the connection timeout parameter, disabling timout");
            }
        }

        String bindDn = serverConfig.getBindDn();
        if (bindDn != null) {
            // Authenticated connection
            props.put(Context.SECURITY_PRINCIPAL, bindDn);
            props.put(Context.SECURITY_CREDENTIALS,
                    serverConfig.getBindPassword());
        }

        if (serverConfig.isPoolingEnabled()) {
            // Enable connection pooling
            props.put("com.sun.jndi.ldap.connect.pool", "true");
            props.put("com.sun.jndi.ldap.connect.pool.protocol", "plain ssl");
            props.put("com.sun.jndi.ldap.connect.pool.authentication",
                    "none simple DIGEST-MD5");
            props.put("com.sun.jndi.ldap.connect.pool.timeout", "1800000"); // 30
            // min
        }

        if (!serverConfig.isVerifyServerCert() && serverConfig.useSsl) {
            props.put("java.naming.ldap.factory.socket",
                    "org.nuxeo.ecm.directory.ldap.LDAPDirectory$TrustingSSLSocketFactory");
        }

        return props;
    }

    public Properties getContextProperties() {
        return contextProperties;
    }

    /**
     * Search controls that only fetch attributes defined by the schema
     *
     * @return common search controls to use for all LDAP search queries
     * @throws DirectoryException
     */
    protected SearchControls computeSearchControls() throws DirectoryException {
        SearchControls scts = new SearchControls();
        // respect the scope of the configuration
        scts.setSearchScope(config.getSearchScope());

        // only fetch attributes that are defined in the schema or needed to
        // compute LDAPReferences
        Set<String> attrs = new HashSet<String>();
        for (String fieldName : schemaFieldMap.keySet()) {
            if (!references.containsKey(fieldName)) {
                attrs.add(fieldMapper.getBackendField(fieldName));
            }
        }
        attrs.add("objectClass");

        for (Reference reference : references.values()) {
            if (reference instanceof LDAPReference) {
                LDAPReference ldapReference = (LDAPReference) reference;
                attrs.add(ldapReference.getStaticAttributeId(fieldMapper));
                attrs.add(ldapReference.getDynamicAttributeId());

                // Add Dynamic Reference attributes filtering
                for (LDAPDynamicReferenceDescriptor dynAtt : ldapReference.getDynamicAttributes()) {
                    attrs.add(dynAtt.baseDN);
                    attrs.add(dynAtt.filter);
                }

            }
        }

        if (config.getPasswordField() != null) {
            // never try to fetch the password
            attrs.remove(config.getPasswordField());
        }

        scts.setReturningAttributes(attrs.toArray(new String[attrs.size()]));

        scts.setCountLimit(config.getQuerySizeLimit());
        scts.setTimeLimit(config.getQueryTimeLimit());

        return scts;
    }

    public SearchControls getSearchControls() {
        return getSearchControls(false);
    }

    public SearchControls getSearchControls(boolean fetchAllAttributes) {
        if (fetchAllAttributes) {
            // build a new ftcs instance with no attribute filtering
            SearchControls scts = new SearchControls();
            scts.setSearchScope(config.getSearchScope());
            return scts;
        } else {
            // return the precomputed scts instance
            return searchControls;
        }
    }

    protected DirContext createContext() throws DirectoryException {
        try {
            /*
             * Dynamic server list requires re-computation on each access
             */
            String serverName = config.getServerName();
            if (serverName == null || serverName.equals("")) {
                throw new DirectoryException(
                        "server configuration is missing for directory "
                                + config.getName());
            }
            LDAPServerDescriptor serverConfig = getServer();
            if (serverConfig.isDynamicServerList()) {
                String ldapUrls = serverConfig.getLdapUrls();
                contextProperties.put(Context.PROVIDER_URL, ldapUrls);
            }
            return new InitialDirContext(contextProperties);
        } catch (NamingException e) {
            throw new DirectoryException("Cannot connect to LDAP directory '"
                    + getName() + "': " + e.getMessage(), e);
        }
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public String getSchema() {
        return config.getSchemaName();
    }

    @Override
    public String getParentDirectory() {
        return null; // no parent directories are specified for LDAP
    }

    @Override
    public String getIdField() {
        return config.getIdField();
    }

    @Override
    public String getPasswordField() {
        return config.getPasswordField();
    }

    /**
     * @since 5.7
     * @return ldap server descriptor bound to this directory
     */
    public LDAPServerDescriptor getServer() {
        return factory.getServer(config.getServerName());
    }

    @Override
    public Session getSession() throws DirectoryException {
        if(schemaFieldMap == null)
        {
            initLDAPConfig();
        }
        DirContext context;
        if (testServer != null) {
            context = testServer.getContext();
        } else {
            context = createContext();
        }
        Session session = new LDAPSession(this, context);
        addSession(session);
        return SecuredSession.wrap(this, config.permissions, session);
    }

    public String getBaseFilter() {
        // NXP-2461: always add control on id field in base filter
        String idField = getIdField();
        DirectoryFieldMapper fieldMapper = getFieldMapper();
        String idAttribute = fieldMapper.getBackendField(idField);
        String idFilter = String.format("(%s=*)", idAttribute);
        if (baseFilter != null && !"".equals(baseFilter)) {
            if (baseFilter.startsWith("(")) {
                return String.format("(&%s%s)", baseFilter, idFilter);
            } else {
                return String.format("(&(%s)%s)", baseFilter, idFilter);
            }
        } else {
            return idFilter;
        }
    }

    public LDAPDirectoryDescriptor getConfig() {
        return config;
    }

    public Map<String, Field> getSchemaFieldMap() {
        return schemaFieldMap;
    }

    /**
     * Get the value of the field passwordHashAlgorithm
     *
     * @return The value
     *
     * @since 5.9.3
     */
    public String getPasswordHashAlgorithmField() {
        return config.getPasswordHashAlgorithmField();
    }

    public void setTestServer(ContextProvider testServer) {
        this.testServer = testServer;
    }

    /**
     * SSLSocketFactory implementation that verifies all certificates.
     */
    public static class TrustingSSLSocketFactory extends SSLSocketFactory {

        private SSLSocketFactory factory;

        /**
         * Create a new SSLSocketFactory that creates a Socket regardless of the
         * certificate used.
         *
         * @throws SSLException if initialization fails.
         */
        public TrustingSSLSocketFactory() {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null,
                        new TrustManager[] { new TrustingX509TrustManager() },
                        new SecureRandom());
                factory = sslContext.getSocketFactory();
            } catch (NoSuchAlgorithmException nsae) {
                throw new RuntimeException(
                        "Unable to initialize the SSL context:  ", nsae);
            } catch (KeyManagementException kme) {
                throw new RuntimeException(
                        "Unable to register a trust manager:  ", kme);
            }
        }

        /**
         * TrustingSSLSocketFactoryHolder is loaded on the first execution of
         * TrustingSSLSocketFactory.getDefault() or the first access to
         * TrustingSSLSocketFactoryHolder.INSTANCE, not before.
         */
        private static class TrustingSSLSocketFactoryHolder {
            public static final TrustingSSLSocketFactory INSTANCE = new TrustingSSLSocketFactory();
        }

        public static SocketFactory getDefault() {
            return TrustingSSLSocketFactoryHolder.INSTANCE;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return factory.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return factory.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket s, String host, int port,
                boolean autoClose) throws IOException {
            return factory.createSocket(s, host, port, autoClose);
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException,
                UnknownHostException {
            return factory.createSocket(host, port);
        }

        @Override
        public Socket createSocket(InetAddress host, int port)
                throws IOException {
            return factory.createSocket(host, port);
        }

        @Override
        public Socket createSocket(String host, int port,
                InetAddress localHost, int localPort) throws IOException,
                UnknownHostException {
            return factory.createSocket(host, port, localHost, localPort);
        }

        @Override
        public Socket createSocket(InetAddress address, int port,
                InetAddress localAddress, int localPort) throws IOException {
            return factory.createSocket(address, port, localAddress, localPort);
        }

        /**
         * Insecurely trusts everyone.
         */
        private class TrustingX509TrustManager implements X509TrustManager {

            @Override
            public void checkClientTrusted(X509Certificate[] arg0, String arg1)
                    throws CertificateException {
                return;
            }

            @Override
            public void checkServerTrusted(X509Certificate[] arg0, String arg1)
                    throws CertificateException {
                return;
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[0];
            }
        }

    }

}
