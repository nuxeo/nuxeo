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
 * $Id: RelationManagerBean.java 22131 2007-07-06 17:53:42Z gracinet $
 */

package org.nuxeo.ecm.platform.relation.ejb;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QueryResult;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.ejb.RelationManagerLocal;
import org.nuxeo.ecm.platform.relations.api.ejb.RelationManagerRemote;
import org.nuxeo.runtime.api.Framework;

/**
 * Relations bean
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
@Stateless
@Local(RelationManagerLocal.class)
@Remote(RelationManagerRemote.class)
public class RelationManagerBean implements RelationManager {

    private static final long serialVersionUID = -4778456059717447736L;

    private static final Log log = LogFactory.getLog(RelationManagerBean.class);

    private transient RelationManager service;

    @PostConstruct
    public void initialize() {
        try {
            // get Runtime service
            service = Framework.getLocalService(RelationManager.class);
        } catch (Exception e) {
            log.error("Could not get relation service", e);
        }
    }

    public void remove() {
    }

    // TODO: maybe hack here to get graph in a cleaner way
    public Graph getGraphByName(String name) throws ClientException {
        try {
            return service.getGraphByName(name);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public Serializable getResourceRepresentation(String namespace,
            Resource resource, Map<String, Serializable> context)
            throws ClientException {
        try {
            return service.getResourceRepresentation(namespace, resource,
                    context);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public Resource getResource(String namespace, Serializable object,
            Map<String, Serializable> context) throws ClientException {
        try {
            return service.getResource(namespace, object, context);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public Set<Resource> getAllResources(Serializable object,
            Map<String, Serializable> context) throws ClientException {
        try {
            return service.getAllResources(object, context);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public List<String> getGraphNames() throws ClientException {
        try {
            return service.getGraphNames();
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public void add(String graphName, List<Statement> statements)
            throws ClientException {
        try {
            getGraphByName(graphName).add(statements);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public void clear(String graphName) throws ClientException {
        try {
            getGraphByName(graphName).clear();
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public List<Node> getObjects(String graphName, Node subject, Node predicate)
            throws ClientException {
        try {
            return getGraphByName(graphName).getObjects(subject, predicate);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public List<Node> getPredicates(String graphName, Node subject, Node object)
            throws ClientException {
        try {
            return getGraphByName(graphName).getPredicates(subject, object);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public List<Statement> getStatements(String graphName, Statement statement)
            throws ClientException {
        try {
            return getGraphByName(graphName).getStatements(statement);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public List<Statement> getStatements(String graphName)
            throws ClientException {
        try {
            return getGraphByName(graphName).getStatements();
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public List<Node> getSubjects(String graphName, Node predicate, Node object)
            throws ClientException {
        try {
            return getGraphByName(graphName).getSubjects(predicate, object);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public boolean hasResource(String graphName, Resource resource)
            throws ClientException {
        try {
            return getGraphByName(graphName).hasResource(resource);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public boolean hasStatement(String graphName, Statement statement)
            throws ClientException {
        try {
            return getGraphByName(graphName).hasStatement(statement);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public QueryResult query(String graphName, String queryString,
            String language, String baseURI) throws ClientException {
        try {
            return getGraphByName(graphName).query(queryString, language,
                    baseURI);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public boolean read(String graphName, InputStream in, String lang,
            String base) throws ClientException {
        try {
            return getGraphByName(graphName).read(in, lang, base);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public void remove(String graphName, List<Statement> statements)
            throws ClientException {
        try {
            getGraphByName(graphName).remove(statements);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public Long size(String graphName) throws ClientException {
        try {
            return getGraphByName(graphName).size();
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public boolean write(String graphName, OutputStream out, String lang,
            String base) throws ClientException {
        try {
            return getGraphByName(graphName).write(out, lang, base);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public Graph getTransientGraph(String type) throws ClientException {
        return service.getTransientGraph(type);
    }

}
