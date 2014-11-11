/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nuxeo.ecm.core.jca;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.query.Query;
import org.nuxeo.ecm.core.query.QueryException;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.security.SecurityManager;

/**
 * The JCA connection wraps a Session.
 * <p>
 * These sources are based on the JackRabbit JCA implementation
 * (http://jackrabbit.apache.org/)
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public final class JCAConnection implements Session {

    private static final Log log = LogFactory.getLog(JCAConnection.class);

    /**
     * Session instance.
     */
    private final Session session;

    /**
     * Managed connection.
     */
    private JCAManagedConnection mc;

    /**
     * Constructs a new session handle.
     */
    public JCAConnection(JCAManagedConnection mc, Session session) {
        this.mc = mc;
        this.session = session;
    }

    /**
     * Returns the managed connection.
     */
    public JCAManagedConnection getManagedConnection() {
        return mc;
    }

    /**
     * Sets the managed connection.
     */
    public void setManagedConnection(JCAManagedConnection mc) {
        this.mc = mc;
    }

    /**
     * Returns the session.
     */
    public Session getSession() {
        return session;
    }

    public long getSessionId() {
        return session.getSessionId();
    }

    public String getUserSessionId() {
        return session.getUserSessionId();
    }

    /**
     * Returns the repository.
     */
    public Repository getRepository() {
        return session.getRepository();
    }

    public void close() throws DocumentException {
        // delegate this to the managed connection
        mc.closeHandle(this);
    }

    public void dispose() {
        // delegate this to the managed connection
        try {
            mc.closeHandle(this);
        } catch (DocumentException e) {
            log.error("error closing session");
        }
    }

    public Map<String, Serializable> getSessionContext() {
        return session.getSessionContext();
    }

    public XAResource getXAResource() {
        return session.getXAResource();
    }

    public SchemaManager getTypeManager() {
        return session.getTypeManager();
    }

    public SecurityManager getSecurityManager() {
        return session.getSecurityManager();
    }

    public Query createQuery(String query, Query.Type qType, String... params)
            throws QueryException {
        return session.createQuery(query, qType, params);
    }

    public IterableQueryResult queryAndFetch(String query, String queryType,
            QueryFilter queryFilter, Object... params) throws QueryException {
        return session.queryAndFetch(query, queryType, queryFilter, params);
    }

    public Document resolvePath(String path) throws DocumentException {
        return session.resolvePath(path);
    }

    public Document getDocumentByUUID(String uuid) throws DocumentException {
        return session.getDocumentByUUID(uuid);
    }

    public void save() throws DocumentException {
        session.save();
    }

    public void cancel() throws DocumentException {
        session.cancel();
    }

    public boolean isLive() {
        return session.isLive();
    }

    public Document getRootDocument() throws DocumentException {
        return session.getRootDocument();
    }

    public Document copy(Document src, Document dst, String name)
            throws DocumentException {
        return session.copy(src, dst, name);
    }

    public Document move(Document src, Document dst, String name)
            throws DocumentException {
        return session.move(src, dst, name);
    }

    public InputStream getDataStream(String key) throws DocumentException {
        return session.getDataStream(key);
    }

    public Document createProxy(Document doc, Document folder)
            throws DocumentException {
        return session.createProxy(doc, folder);
    }

    public Document createProxyForVersion(Document parent, Document doc,
            String versionLabel) throws DocumentException {
        return session.createProxyForVersion(parent, doc, versionLabel);
    }

    public Collection<Document> getProxies(Document doc, Document folder)
            throws DocumentException {
        return session.getProxies(doc, folder);
    }

    public Document importDocument(String uuid, Document parent, String name,
            String typeName, Map<String, Serializable> props)
            throws DocumentException {
        return session.importDocument(uuid, parent, name, typeName, props);
    }

    public Document getVersion(String versionableId, VersionModel versionModel)
            throws DocumentException {
        return session.getVersion(versionableId, versionModel);
    }

    @Override
    public String toString() {
        return System.identityHashCode(this) + ":" + session.toString();
    }

}
