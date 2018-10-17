/*
 * Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
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
 * This file has been modified to work in the NXLDAPDirectory test setup.
 * The original file can be found at the following URL:
 *
 *  https://svn.sourceforge.net/svnroot/acegisecurity/trunk/acegisecurity/
 *     core/src/test/java/org/acegisecurity/ldap/LdapTestServer.java
 */

package org.nuxeo.ecm.directory.ldap;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.ldap.InitialLdapContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.directory.server.core.configuration.Configuration;
import org.apache.directory.server.core.configuration.MutablePartitionConfiguration;
import org.apache.directory.server.core.configuration.MutableStartupConfiguration;
import org.apache.directory.server.core.configuration.PartitionConfiguration;
import org.apache.directory.server.core.configuration.ShutdownConfiguration;
import org.apache.directory.server.core.jndi.CoreContextFactory;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.prefs.ServerSystemPreferenceException;

/**
 * An embedded LDAP test server, complete with test data for running the unit tests against.
 *
 * @author Luke Taylor
 * @version $Id: LdapTestServer.java 1496 2006-05-23 13:38:33Z benalex $
 */
public class MockLdapServer implements ContextProvider {
    private static final String BASE_DN = "dc=example,dc=com";

    // ~ Instance fields
    // ================================================================================================
    private static final Log log = LogFactory.getLog(MockLdapServer.class);

    private DirContext serverContext;

    // Move the working dir to the temp directory
    private File workingDir;

    private MutableStartupConfiguration cfg;

    // ~ Constructors
    // ===================================================================================================

    /**
     * Starts up and configures ApacheDS.
     */
    public MockLdapServer(File basedir) {
        workingDir = new File(basedir, "apacheds");
        workingDir.delete();
        workingDir.mkdirs();
        startLdapServer();
    }

    // ~ Methods
    // ========================================================================================================

    public void createGroup(String cn, String ou, String[] memberDns) {
        Attributes group = new BasicAttributes("cn", cn);
        Attribute members = new BasicAttribute("member");
        Attribute orgUnit = new BasicAttribute("ou", ou);

        for (String memberDn : memberDns) {
            members.add(memberDn);
        }

        Attribute objectClass = new BasicAttribute("objectClass");
        objectClass.add("top");
        objectClass.add("groupOfNames");

        group.put(objectClass);
        group.put(members);
        group.put(orgUnit);

        try {
            serverContext.createSubcontext("cn=" + cn + ",ou=groups", group);
        } catch (NameAlreadyBoundException ignore) {
            // System.out.println(" group " + cn + " already exists.");
        } catch (NamingException ne) {
            log.error("Failed to create group", ne);
        }
    }

    public void createManagerUser() {
        Attributes user = new BasicAttributes("cn", "manager", true);
        user.put("userPassword", "secret");

        Attribute objectClass = new BasicAttribute("objectClass");
        user.put(objectClass);
        objectClass.add("top");
        objectClass.add("person");
        objectClass.add("organizationalPerson");
        objectClass.add("inetOrgPerson");
        user.put("sn", "Manager");
        user.put("cn", "manager");

        try {
            serverContext.createSubcontext("cn=manager", user);
        } catch (NameAlreadyBoundException ignore) {
            log.warn("Manager user already exists.");
        } catch (NamingException ne) {
            log.error("Failed to create manager user", ne);
        }
    }

    public void createOu(String name) {
        Attributes ou = new BasicAttributes("ou", name);
        Attribute objectClass = new BasicAttribute("objectClass");
        objectClass.add("top");
        objectClass.add("organizationalUnit");
        ou.put(objectClass);

        try {
            serverContext.createSubcontext("ou=" + name, ou);
        } catch (NameAlreadyBoundException ignore) {
            log.warn("ou " + name + " already exists.");
        } catch (NamingException ne) {
            log.error("Failed to create ou: ", ne);
        }
    }

    public void createUser(String uid, String cn, String password) {
        Attributes user = new BasicAttributes("uid", uid);
        user.put("cn", cn);
        user.put("userPassword", password);

        Attribute objectClass = new BasicAttribute("objectClass");
        user.put(objectClass);
        objectClass.add("top");
        objectClass.add("person");
        objectClass.add("organizationalPerson");
        objectClass.add("inetOrgPerson");
        user.put("sn", uid);

        try {
            serverContext.createSubcontext("uid=" + uid + ",ou=people", user);
        } catch (NameAlreadyBoundException ignore) {
            // System.out.println(" user " + uid + " already exists.");
        } catch (NamingException ne) {
            System.err.println("Failed to create user.");
            ne.printStackTrace();
        }
    }

    public Configuration getConfiguration() {
        return cfg;
    }

    @Override
    public DirContext getContext() {
        // ensure the context server is not closed
        startLdapServer();
        return serverContext;
    }

    private void initConfiguration() throws NamingException {
        MutablePartitionConfiguration systemPartition = new MutablePartitionConfiguration();
        systemPartition.setId(PartitionConfiguration.SYSTEM_PARTITION_NAME);
        systemPartition.setSuffix("ou=system");
        systemPartition.setIndexedAttributes(Collections.singleton("objectClass"));
        cfg.setSystemPartitionConfiguration(systemPartition);

        // Create the partition for the tests
        MutablePartitionConfiguration testPartition = new MutablePartitionConfiguration();
        testPartition.setId("NuxeoTestLdapServer");
        testPartition.setSuffix(BASE_DN);

        BasicAttributes attributes = new BasicAttributes();
        BasicAttribute objectClass = new BasicAttribute("objectClass");
        objectClass.add("top");
        objectClass.add("domain");
        objectClass.add("extensibleObject");
        attributes.put(objectClass);
        testPartition.setContextEntry(attributes);

        Set<Object> indexedAttrs = new HashSet<Object>();
        indexedAttrs.add("objectClass");
        indexedAttrs.add("uid");
        indexedAttrs.add("cn");
        indexedAttrs.add("ou");
        indexedAttrs.add("uniqueMember");

        // POSIX RFC-2307 schema.
        indexedAttrs.add("gidNumber");
        indexedAttrs.add("uidNumber");

        testPartition.setIndexedAttributes(indexedAttrs);

        Set<MutablePartitionConfiguration> partitions = new HashSet<MutablePartitionConfiguration>();
        partitions.add(testPartition);

        cfg.setPartitionConfigurations(partitions);
    }

    public void startLdapServer() {
        cfg = new MutableStartupConfiguration();
        cfg.setWorkingDirectory(workingDir);

        log.debug("Working directory is " + workingDir.getAbsolutePath());

        Properties env = new Properties();

        env.setProperty(Context.PROVIDER_URL, BASE_DN);
        env.setProperty(Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName());
        env.setProperty(Context.SECURITY_AUTHENTICATION, "simple");
        env.setProperty(Context.SECURITY_PRINCIPAL, PartitionNexus.ADMIN_PRINCIPAL);
        env.setProperty(Context.SECURITY_CREDENTIALS, PartitionNexus.ADMIN_PASSWORD);

        try {
            initConfiguration();
            env.putAll(cfg.toJndiEnvironment());
            serverContext = new InitialDirContext(env);
        } catch (NamingException e) {
            log.error("Failed to start Apache DS: ", e);
        }
    }

    public void shutdownLdapServer() {

        Hashtable<String, Object> env = new Hashtable<>(new ShutdownConfiguration().toJndiEnvironment());
        env.put(Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName());
        env.put(Context.PROVIDER_URL, BASE_DN);

        try {
            new InitialLdapContext(env, null);
        } catch (Exception e) {
            throw new ServerSystemPreferenceException("Failed to shutdown ldap server.", e);
        }
    }
}
