/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.mbean;

import java.util.LinkedList;
import java.util.List;

import javax.naming.Binding;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.storage.sql.RepositoryManagement;

/**
 * An MBean to manage SQL storage repositories.
 *
 * @author Florent Guillaume
 */
public class RepositoryStatus implements RepositoryStatusMBean {

    private static final Log log = LogFactory.getLog(RepositoryStatus.class);

    protected List<RepositoryManagement> getRepositories()
            throws NamingException {
        LinkedList<RepositoryManagement> list = new LinkedList<RepositoryManagement>();
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
        return list;
    }

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

}
