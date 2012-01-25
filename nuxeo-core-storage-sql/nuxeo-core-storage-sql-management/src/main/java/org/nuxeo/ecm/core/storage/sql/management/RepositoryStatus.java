/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.management;

import java.util.LinkedList;
import java.util.List;

import javax.naming.Binding;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.storage.sql.RepositoryManagement;
import org.nuxeo.ecm.core.storage.sql.net.MapperClientInfo;

/**
 * An MBean to manage SQL storage repositories.
 *
 * @author Florent Guillaume
 */
public class RepositoryStatus implements RepositoryStatusMBean {

    private static final Log log = LogFactory.getLog(RepositoryStatus.class);

    protected List<RepositoryManagement> getRepositories() throws NamingException {
        List<RepositoryManagement> list = new LinkedList<RepositoryManagement>();
        InitialContext context;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(RepositoryStatus.class.getClassLoader());
        try {
            context = new InitialContext();
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
        // we search both JBoss-like and Glassfish-like prefixes
        // @see NXCore#getRepository
        for (String prefix : new String[] { "java:NXRepository", "NXRepository" }) {
            NamingEnumeration<Binding> bindings;
            try {
                bindings = context.listBindings(prefix);
            } catch (NamingException e) {
                continue;
            }
            for (NamingEnumeration<Binding> e = bindings; e.hasMore();) {
                Binding binding = e.nextElement();
                String name = binding.getName();
                if (binding.isRelative()) {
                    name = prefix + '/' + name;
                }
                Object object = context.lookup(name);
                if (!(object instanceof RepositoryManagement)) {
                    continue;
                }
                list.add((RepositoryManagement) object);
            }
        }
        return list;
    }

    @Override
    public String listActiveSessions() {
        List<RepositoryManagement> repositories;
        try {
            repositories = getRepositories();
        } catch (NamingException e) {
            log.error("Error getting repositories", e);
            return "Error!";
        }
        StringBuilder buf = new StringBuilder();
        buf.append("Actives sessions for SQL repositories:<br />");
        for (RepositoryManagement repository : repositories) {
            buf.append("<b>").append(repository.getName()).append("</b>: ");
            buf.append(repository.getActiveSessionsCount());
            buf.append("<br />");
        }
        return buf.toString();
    }

    @Override
    public int getActiveSessionsCount() {
        List<RepositoryManagement> repositories;
        try {
            repositories = getRepositories();
        } catch (NamingException e) {
            throw new IllegalStateException("Cannot get repositories", e);
        }
        int count = 0;
        for (RepositoryManagement repository : repositories) {
            count += repository.getActiveSessionsCount();
        }
        return count;
    }

    @Override
    public String clearCaches() {
        List<RepositoryManagement> repositories;
        try {
            repositories = getRepositories();
        } catch (NamingException e) {
            log.error("Error getting repositories", e);
            return "Error!";
        }
        StringBuilder buf = new StringBuilder();
        buf.append("Cleared cached objects for SQL repositories:<br />");
        for (RepositoryManagement repository : repositories) {
            buf.append("<b>").append(repository.getName()).append("</b>: ");
            buf.append(repository.clearCaches());
            buf.append("<br />");
        }
        return buf.toString();
    }

    @Override
    public String listRemoteSessions() {
        List<RepositoryManagement> repositories;
        try {
            repositories = getRepositories();
        } catch (NamingException e) {
            log.error("Error getting repositories", e);
            return "Error!";
        }
        StringBuilder buf = new StringBuilder();
        buf.append("<dl>").append("\n");
        for (RepositoryManagement repository : repositories) {
            buf.append("<dt class='repository'>").append(repository.getName()).append("</dt>").append("\n");
            buf.append("<dd>").append("\n");
            buf.append(" <dl>").append("\n");
            buf.append("  <dt>location</dt>");
            buf.append("  <dd class='location'>").append(repository.getServerURL()).append("</dd>");
            for (MapperClientInfo info : repository.getClientInfos()) {
                buf.append("  <dt>").append(info.getRemoteUser()).append("  </dt>");
                buf.append("  <dd>").append(info.getRemoteIP()).append("  </dd>").append("\n");
            }
            buf.append(" </dl>").append("\n");
            buf.append("</dd>").append("\n");
        }
        buf.append("</dl>").append("\n");
        return buf.toString();
    }

}
