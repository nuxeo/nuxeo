/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.ra;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.Interaction;
import javax.resource.cci.LocalTransaction;
import javax.resource.cci.ResultSetInfo;

import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.model.LockManager;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Node;
import org.nuxeo.ecm.core.storage.sql.Session;
import org.nuxeo.ecm.core.storage.sql.SessionImpl;

/**
 * A connection is a handle to the underlying storage. It is returned by the {@link ConnectionFactory} to application
 * code.
 * <p>
 * The actual link to the underlying storage ({@link Session}) is provided by the
 * {@link javax.resource.spi.ManagedConnection} which created this {@link javax.resource.cci.Connection}.
 *
 * @author Florent Guillaume
 */
public class ConnectionImpl implements Session {

    private ManagedConnectionImpl managedConnection;

    private SessionImpl session;

    public ConnectionImpl(ManagedConnectionImpl managedConnection) {
        this.managedConnection = managedConnection;
    }

    /*
     * ----- callbacks -----
     */

    /**
     * Called by {@link ManagedConnectionImpl#associateConnection}.
     */
    protected ManagedConnectionImpl getManagedConnection() {
        return managedConnection;
    }

    /**
     * Called by {@link ManagedConnectionImpl#associateConnection}.
     */
    protected void setManagedConnection(ManagedConnectionImpl managedConnection) {
        this.managedConnection = managedConnection;
    }

    /**
     * Called by {@link ManagedConnectionImpl#addConnection}.
     */
    protected void associate(SessionImpl session) {
        this.session = session;
    }

    /**
     * Called by {@link ManagedConnectionImpl#removeConnection}.
     */
    protected void disassociate() {
        closeStillOpenQueryResults();
        session = null;
    }

    /*
     * ----- javax.resource.cci.Connection -----
     */

    protected Throwable closeTrace;

    @Override
    public void close() throws ResourceException {
        if (managedConnection == null) {
            IllegalStateException error = new IllegalStateException("connection already closed " + this);
            error.addSuppressed(closeTrace);
            throw error;
        }
        try {
            managedConnection.close(this);
        } finally {
            closeTrace = new Throwable("close stack trace");
            managedConnection = null;
        }
    }

    @Override
    public Interaction createInteraction() throws ResourceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectionMetaData getMetaData() throws ResourceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResultSetInfo getResultSetInfo() throws ResourceException {
        throw new UnsupportedOperationException();
    }

    /*
     * ----- org.nuxeo.ecm.core.storage.sql.Session -----
     */

    private Session getSession() {
        if (session == null) {
            throw new NuxeoException("Cannot use closed connection handle: " + this);
        }
        return session;
    }

    @Override
    public Mapper getMapper() {
        return getSession().getMapper();
    }

    @Override
    public boolean isLive() {
        return session != null && session.isLive();
    }

    @Override
    public String getRepositoryName() {
        return getSession().getRepositoryName();
    }

    @Override
    public Model getModel() {
        return getSession().getModel();
    }

    @Override
    public void save() {
        getSession().save();
    }

    @Override
    public Node getRootNode() {
        return getSession().getRootNode();
    }

    @Override
    public Node getNodeById(Serializable id) {
        return getSession().getNodeById(id);
    }

    @Override
    public List<Node> getNodesByIds(Collection<Serializable> ids) {
        return getSession().getNodesByIds(ids);
    }

    @Override
    public Node getNodeByPath(String path, Node node) {
        return getSession().getNodeByPath(path, node);
    }

    @Override
    public boolean addMixinType(Node node, String mixin) {
        return getSession().addMixinType(node, mixin);
    }

    @Override
    public boolean removeMixinType(Node node, String mixin) {
        return getSession().removeMixinType(node, mixin);
    }

    @Override
    public ScrollResult<String> scroll(String query, int batchSize, int keepAliveSeconds) {
        return getSession().scroll(query, batchSize, keepAliveSeconds);
    }

    @Override
    public ScrollResult<String> scroll(String scrollId) {
        return getSession().scroll(scrollId);
    }

    @Override
    public boolean hasChildNode(Node parent, String name, boolean complexProp) {
        return getSession().hasChildNode(parent, name, complexProp);
    }

    @Override
    public Node getChildNode(Node parent, String name, boolean complexProp) {
        return getSession().getChildNode(parent, name, complexProp);
    }

    @Override
    public boolean hasChildren(Node parent, boolean complexProp) {
        return getSession().hasChildren(parent, complexProp);
    }

    @Override
    public List<Node> getChildren(Node parent, String name, boolean complexProp) {
        return getSession().getChildren(parent, name, complexProp);
    }

    @Override
    public Node addChildNode(Node parent, String name, Long pos, String typeName, boolean complexProp) {
        return getSession().addChildNode(parent, name, pos, typeName, complexProp);
    }

    @Override
    public Node addChildNode(Serializable id, Node parent, String name, Long pos, String typeName,
            boolean complexProp) {
        return getSession().addChildNode(id, parent, name, pos, typeName, complexProp);
    }

    @Override
    public void removeNode(Node node) {
        getSession().removeNode(node);
    }

    @Override
    public void removePropertyNode(Node node) {
        getSession().removePropertyNode(node);
    }

    @Override
    public Node getParentNode(Node node) {
        return getSession().getParentNode(node);
    }

    @Override
    public String getPath(Node node) {
        return getSession().getPath(node);
    }

    @Override
    public void orderBefore(Node node, Node src, Node dest) {
        getSession().orderBefore(node, src, dest);
    }

    @Override
    public Node move(Node source, Node parent, String name) {
        return getSession().move(source, parent, name);
    }

    @Override
    public Node copy(Node source, Node parent, String name) {
        return getSession().copy(source, parent, name);
    }

    @Override
    public Node checkIn(Node node, String label, String checkinComment) {
        return getSession().checkIn(node, label, checkinComment);
    }

    @Override
    public void checkOut(Node node) {
        getSession().checkOut(node);
    }

    @Override
    public void restore(Node node, Node version) {
        getSession().restore(node, version);
    }

    @Override
    public Node getVersionByLabel(Serializable versionSeriesId, String label) {
        return getSession().getVersionByLabel(versionSeriesId, label);
    }

    @Override
    public List<Node> getVersions(Serializable versionSeriesId) {
        return getSession().getVersions(versionSeriesId);
    }

    @Override
    public Node getLastVersion(Serializable versionSeriesId) {
        return getSession().getLastVersion(versionSeriesId);
    }

    @Override
    public List<Node> getProxies(Node document, Node parent) {
        return getSession().getProxies(document, parent);
    }

    @Override
    public List<Node> getProxies(Node document) {
        return getSession().getProxies(document);
    }

    @Override
    public void setProxyTarget(Node proxy, Serializable targetId) {
        getSession().setProxyTarget(proxy, targetId);
    }

    @Override
    public Node addProxy(Serializable targetId, Serializable versionSeriesId, Node parent, String name, Long pos) {
        return getSession().addProxy(targetId, versionSeriesId, parent, name, pos);
    }

    @Override
    public PartialList<Serializable> query(String query, QueryFilter queryFilter, boolean countTotal) {
        return getSession().query(query, queryFilter, countTotal);
    }

    @Override
    public PartialList<Serializable> query(String query, String queryType, QueryFilter queryFilter, long countUpTo) {
        return getSession().query(query, queryType, queryFilter, countUpTo);
    }

    @Override
    public IterableQueryResult queryAndFetch(String query, String queryType, QueryFilter queryFilter,
            Object... params) {
        IterableQueryResult result = getSession().queryAndFetch(query, queryType, queryFilter, params);
        noteQueryResult(result);
        return result;
    }

    @Override
    public IterableQueryResult queryAndFetch(String query, String queryType, QueryFilter queryFilter,
            boolean distinctDocuments, Object... params) {
        IterableQueryResult result = getSession().queryAndFetch(query, queryType, queryFilter, distinctDocuments,
                params);
        noteQueryResult(result);
        return result;
    }

    @Override
    public PartialList<Map<String,Serializable>> queryProjection(String query, String queryType, QueryFilter queryFilter,
            boolean distinctDocuments, long countUpTo, Object... params) {
        return getSession().queryProjection(query, queryType, queryFilter, distinctDocuments, countUpTo, params);
    }

    public static class QueryResultContextException extends Exception {
        private static final long serialVersionUID = 1L;

        public final IterableQueryResult queryResult;

        public QueryResultContextException(IterableQueryResult queryResult) {
            super("queryAndFetch call context");
            this.queryResult = queryResult;
        }
    }

    protected final Set<QueryResultContextException> queryResults = new HashSet<>();

    protected void noteQueryResult(IterableQueryResult result) {
        queryResults.add(new QueryResultContextException(result));
    }

    protected void closeStillOpenQueryResults() {
        for (QueryResultContextException context : queryResults) {
            if (!context.queryResult.mustBeClosed()) {
                continue;
            }
            try {
                context.queryResult.close();
            } catch (RuntimeException e) {
                LogFactory.getLog(ConnectionImpl.class).error("Cannot close query result", e);
            } finally {
                LogFactory.getLog(ConnectionImpl.class)
                          .warn("Closing a query results for you, check stack trace for allocating point", context);
            }
        }
        queryResults.clear();
    }

    @Override
    public LockManager getLockManager() {
        return getSession().getLockManager();
    }

    @Override
    public void requireReadAclsUpdate() {
        if (session != null) {
            session.requireReadAclsUpdate();
        }
    }

    @Override
    public void updateReadAcls() {
        getSession().updateReadAcls();
    }

    @Override
    public void rebuildReadAcls() {
        getSession().rebuildReadAcls();
    }

    @Override
    public Map<String, String> getBinaryFulltext(Serializable id) {
        return getSession().getBinaryFulltext(id);
    }

    @Override
    public boolean isChangeTokenEnabled() {
        return getSession().isChangeTokenEnabled();
    }

    @Override
    public void markUserChange(Serializable id) {
        getSession().markUserChange(id);
    }

}
