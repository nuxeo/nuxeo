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

package org.nuxeo.ecm.core.storage.sql.ra;

import java.io.InputStream;
import java.io.Serializable;
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
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.storage.PartialList;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.binary.Binary;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Node;
import org.nuxeo.ecm.core.storage.sql.Session;
import org.nuxeo.ecm.core.storage.sql.SessionImpl;
import org.nuxeo.runtime.services.streaming.FileSource;

/**
 * A connection is a handle to the underlying storage. It is returned by the
 * {@link ConnectionFactory} to application code.
 * <p>
 * The actual link to the underlying storage ({@link Session}) is provided by
 * the {@link javax.resource.spi.ManagedConnection} which created this
 * {@link javax.resource.cci.Connection}.
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

    private Session getSession() throws StorageException {
        if (session == null) {
            throw new StorageException("Cannot use closed connection handle: "
                    + this);
        }
        return session;
    }

    @Override
    public Mapper getMapper() throws StorageException {
        return getSession().getMapper();
    }

    @Override
    public boolean isLive() {
        return session != null && session.isLive();
    }

    @Override
    public boolean isStateSharedByAllThreadSessions() {
        // the JCA semantics is that in the same thread all handles point to the
        // same underlying session
        return true;
    }

    @Override
    public String getRepositoryName() throws StorageException {
        return getSession().getRepositoryName();
    }

    @Override
    public Binary getBinary(InputStream in) throws StorageException {
        return getSession().getBinary(in);
    }

    @Override
    public Binary getBinary(FileSource source) throws StorageException {
        return getSession().getBinary(source);
    }

    @Override
    public Model getModel() throws StorageException {
        return getSession().getModel();
    }

    @Override
    public void save() throws StorageException {
        getSession().save();
    }

    @Override
    public Node getRootNode() throws StorageException {
        return getSession().getRootNode();
    }

    @Override
    public Node getNodeById(Serializable id) throws StorageException {
        return getSession().getNodeById(id);
    }

    @Override
    public List<Node> getNodesByIds(List<Serializable> ids)
            throws StorageException {
        return getSession().getNodesByIds(ids);
    }

    @Override
    public Node getNodeByPath(String path, Node node) throws StorageException {
        return getSession().getNodeByPath(path, node);
    }

    @Override
    public boolean addMixinType(Node node, String mixin)
            throws StorageException {
        return getSession().addMixinType(node, mixin);
    }

    @Override
    public boolean removeMixinType(Node node, String mixin)
            throws StorageException {
        return getSession().removeMixinType(node, mixin);
    }

    @Override
    public boolean hasChildNode(Node parent, String name, boolean complexProp)
            throws StorageException {
        return getSession().hasChildNode(parent, name, complexProp);
    }

    @Override
    public Node getChildNode(Node parent, String name, boolean complexProp)
            throws StorageException {
        return getSession().getChildNode(parent, name, complexProp);
    }

    @Override
    public boolean hasChildren(Node parent, boolean complexProp)
            throws StorageException {
        return getSession().hasChildren(parent, complexProp);
    }

    @Override
    public List<Node> getChildren(Node parent, String name, boolean complexProp)
            throws StorageException {
        return getSession().getChildren(parent, name, complexProp);
    }

    @Override
    public Node addChildNode(Node parent, String name, Long pos,
            String typeName, boolean complexProp) throws StorageException {
        return getSession().addChildNode(parent, name, pos, typeName,
                complexProp);
    }

    @Override
    public Node addChildNode(Serializable id, Node parent, String name,
            Long pos, String typeName, boolean complexProp)
            throws StorageException {
        return getSession().addChildNode(id, parent, name, pos, typeName,
                complexProp);
    }

    @Override
    public void removeNode(Node node) throws StorageException {
        getSession().removeNode(node);
    }

    @Override
    public void removePropertyNode(Node node) throws StorageException {
        getSession().removePropertyNode(node);
    }

    @Override
    public Node getParentNode(Node node) throws StorageException {
        return getSession().getParentNode(node);
    }

    @Override
    public String getPath(Node node) throws StorageException {
        return getSession().getPath(node);
    }

    @Override
    public void orderBefore(Node node, Node src, Node dest)
            throws StorageException {
        getSession().orderBefore(node, src, dest);
    }

    @Override
    public Node move(Node source, Node parent, String name)
            throws StorageException {
        return getSession().move(source, parent, name);
    }

    @Override
    public Node copy(Node source, Node parent, String name)
            throws StorageException {
        return getSession().copy(source, parent, name);
    }

    @Override
    public Node checkIn(Node node, String label, String checkinComment)
            throws StorageException {
        return getSession().checkIn(node, label, checkinComment);
    }

    @Override
    public void checkOut(Node node) throws StorageException {
        getSession().checkOut(node);
    }

    @Override
    public void restore(Node node, Node version) throws StorageException {
        getSession().restore(node, version);
    }

    @Override
    public Node getVersionByLabel(Serializable versionSeriesId, String label)
            throws StorageException {
        return getSession().getVersionByLabel(versionSeriesId, label);
    }

    @Override
    public List<Node> getVersions(Serializable versionSeriesId)
            throws StorageException {
        return getSession().getVersions(versionSeriesId);
    }

    @Override
    public Node getLastVersion(Serializable versionSeriesId)
            throws StorageException {
        return getSession().getLastVersion(versionSeriesId);
    }

    @Override
    public List<Node> getProxies(Node document, Node parent)
            throws StorageException {
        return getSession().getProxies(document, parent);
    }

    @Override
    public void setProxyTarget(Node proxy, Serializable targetId)
            throws StorageException {
        getSession().setProxyTarget(proxy, targetId);
    }

    @Override
    public Node addProxy(Serializable targetId, Serializable versionSeriesId,
            Node parent, String name, Long pos) throws StorageException {
        return getSession().addProxy(targetId, versionSeriesId, parent, name,
                pos);
    }

    @Override
    public PartialList<Serializable> query(String query,
            QueryFilter queryFilter, boolean countTotal)
            throws StorageException {
        return getSession().query(query, queryFilter, countTotal);
    }

    @Override
    public PartialList<Serializable> query(String query, String queryType,
            QueryFilter queryFilter, long countUpTo)
            throws StorageException {
        return getSession().query(query, queryType, queryFilter, countUpTo);
    }

    @Override
    public IterableQueryResult queryAndFetch(String query, String queryType,
            QueryFilter queryFilter, Object... params) throws StorageException {
        IterableQueryResult result = getSession().queryAndFetch(query, queryType, queryFilter, params);
        noteQueryResult(result);
        return result;
    }

    public static class QueryResultContextException extends Exception {
        private static final long serialVersionUID = 1L;

        public final IterableQueryResult queryResult;

        public QueryResultContextException(IterableQueryResult queryResult) {
            super("queryAndFetch call context");
            this.queryResult = queryResult;
        }
    }

    protected final Set<QueryResultContextException> queryResults = new HashSet<QueryResultContextException>();

    protected void noteQueryResult(IterableQueryResult result) {
        queryResults.add(new QueryResultContextException(result));
    }

    protected void closeStillOpenQueryResults() {
        for (QueryResultContextException context : queryResults) {
            if (!context.queryResult.isLife()) {
                continue;
            }
            try {
                context.queryResult.close();
            } catch (RuntimeException e) {
                LogFactory.getLog(ConnectionImpl.class).error("Cannot close query result", e);
            } finally {
                LogFactory.getLog(ConnectionImpl.class).warn(
                        "Closing a query results for you, check stack trace for allocating point",
                        context);
            }
        }
    }

    @Override
    public Lock getLock(Serializable id) throws StorageException {
        return getSession().getLock(id);
    }

    @Override
    public Lock setLock(Serializable id, Lock lock)
            throws StorageException {
        return getSession().setLock(id, lock);
    }

    @Override
    public Lock removeLock(Serializable id, String owner, boolean force)
            throws StorageException {
        return getSession().removeLock(id, owner, force);
    }

    @Override
    public void requireReadAclsUpdate() {
        if (session != null) {
            session.requireReadAclsUpdate();
        }
    }

    @Override
    public void updateReadAcls() throws StorageException {
        getSession().updateReadAcls();
    }

    @Override
    public void rebuildReadAcls() throws StorageException {
        getSession().rebuildReadAcls();
    }

    @Override
    public Map<String, String> getBinaryFulltext(Serializable id) throws StorageException {
        return getSession().getBinaryFulltext(id);
    }

}
