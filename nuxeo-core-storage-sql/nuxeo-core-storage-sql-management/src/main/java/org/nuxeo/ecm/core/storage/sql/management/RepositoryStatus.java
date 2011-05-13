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
import org.nuxeo.ecm.core.storage.sql.Repository;
import org.nuxeo.ecm.core.storage.sql.RepositoryManagement;
import org.nuxeo.ecm.core.storage.sql.RepositoryResolver;
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
        if (list.size()==0) {
            List<Repository> repos = RepositoryResolver.getRepositories();
            for (Repository repo : repos) {
                list.add(repo);
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
        buf.append("Actives remote session for SQL repositories:<br />");
        for (RepositoryManagement repository : repositories) {
            buf.append("<b>").append(repository.getName()).append("</b>");
            if (repository.getServerURL()==null) {
                buf.append(" Server mode not activated");
            } else {
                buf.append(repository.getServerURL()).append("<br/>");
                if (repository.getClientInfos().size()==0) {
                    buf.append("No client connected").append("<br/>");
                }else {
                    buf.append("<ul>");
                    for (MapperClientInfo info : repository.getClientInfos()) {
                        buf.append("  <li>").append(info.getRemoteUser()).append("  :");
                        buf.append(info.getRemoteIP()).append("  </li>");
                    }
                    buf.append("</ul>");
                }
            }
            buf.append("<br/>");
        }
        return buf.toString();
    }

}
