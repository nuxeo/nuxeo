/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.core.storage.sql.reload;

import java.util.LinkedList;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.naming.Binding;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryManager;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RepositoryReloader implements EventListener {

    private static Log log = LogFactory.getLog(RepositoryReloader.class);

    @Override
    public boolean aboutToHandleEvent(Event event) {
        return true;
    }

    @Override
    public void handleEvent(Event event) {
        final String id = event.getId();
        if ("reloadRepositories".equals(id) || "flush".equals(id)) {
            try {
                reloadRepositories();
            } catch (Exception e) {
                log.error("Failed to reload repositories", e);
            }
        }
    }

    public static List<Repository> getRepositories() throws NamingException {
        List<Repository> list = new LinkedList<Repository>();
        InitialContext context = new InitialContext();
        // we search both JBoss-like and Glassfish-like prefixes
        // @see NXCore#getRepository
        for (String prefix : new String[] { "java:NXRepository", "NXRepository" }) {
            NamingEnumeration<Binding> bindings;
            try {
                bindings = context.listBindings(prefix);
            } catch (NamingException e) {
                continue;
            }
            NamingEnumeration<Binding> e = null;
            try {
                for (e = bindings; e.hasMore();) {
                    Binding binding = e.nextElement();
                    String name = binding.getName();
                    if (binding.isRelative()) {
                        name = prefix + '/' + name;
                    }
                    Object object = context.lookup(name);
                    if (!(object instanceof Repository)) {
                        continue;
                    }
                    list.add((Repository) object);
                }
            } finally {
                if (e != null) {
                    e.close();
                }
            }
        }
        return list;
    }

    public static void closeRepositories() throws Exception {
        List<Repository> repos = getRepositories(); // this is working only on
                                                    // jboss
        if (!repos.isEmpty()) {
            for (Repository repository : repos) {
                repository.shutdown();
            }
        } else { // TODO remove the first method that is using JNDI lookups?
            RepositoryManager mgr = NXCore.getRepositoryService().getRepositoryManager();
            for (String name : mgr.getRepositoryNames()) {
                Repository repo = mgr.getRepository(name);
                repo.shutdown();
            }
        }
    }

    public static MBeanServer locateJBoss() {
        for (MBeanServer server : MBeanServerFactory.findMBeanServer(null)) {
            if (server.getDefaultDomain().equals("jboss")) {
                return server;
            }
        }
        return null;
    }

    public static void flushJCAPool() throws Exception {
        MBeanServer jboss = locateJBoss();
        if (jboss != null) {
            jboss.invoke(
                    new ObjectName(
                            "jboss.jca:name=NXRepository/default,service=ManagedConnectionPool"),
                    "flush", new Object[0], new String[0]);
        } else { // try tomcat (jtajca nuxeo plugin)
            Class<?> cl = null;
            try {
                cl = Class.forName("org.nuxeo.runtime.jtajca.NuxeoContainer");
            } catch (ClassNotFoundException e) { // not tomcat or not jtajca
                                                 // enabled
                // do nothing
            }
            if (cl != null) {
                cl.getMethod("resetConnectionManager").invoke(null);
            }
        }
    }

    /**
     * Reload core repositories.
     *
     * @throws Exception
     */
    public static void reloadRepositories() throws Exception {
        RepositoryReloader.flushJCAPool();
        RepositoryReloader.closeRepositories();
    }
}
