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

package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.repository.DocumentProvider;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.repository.impl.DocumentProviderManager;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.runtime.api.Framework;

/**
 * The CoreInstance is the main access point to a repository server.
 * <p>
 * A server instance is a singleton object that exists on each client JVM but
 * also on the server JVM. The instance on the server JVM is used to access the
 * server locally while those on client JVMs are used to access the server
 * remotely.
 * <p>
 * A server instance uses a CoreSessionFactory to create CoreSession instances.
 * CoreSessionFactory objects are implementation-dependent and may be registered
 * using extension points. See {@link CoreSessionFactory} for more details.
 * <p>
 * Thus you can use a different implementation for the local ServerConnector
 * than the one for the remote ServerConnector.
 * <p>
 * When clients need to perform a connection to a repository, they simply open a
 * new session using the {@link CoreInstance#open(String, Map)} method.
 * <p>
 * When the client has done its work it <b>must</b> close its session by calling
 * {@link CoreInstance#close(CoreSession)}.
 * <p>
 * This ensures correctly freeing all the resources held by the client session.
 * <p>
 * So a client session looks something like this:
 * <p>
 *
 * <pre>
 * &lt;code&gt;
 * CoreInstance server = CoreInstance.getInstance();
 * CoreSession client = server.open(&quot;demo&quot;, null);
 * DocumentModel root = client.getRootDocument();
 * // ... do something in that session ...
 * // close the client -&gt; this is closing the core session
 * server.close(client);
 * &lt;/code&gt;
 * </pre>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class CoreInstance implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(CoreInstance.class);

    private static final CoreInstance instance = new CoreInstance();

    private CoreSessionFactory factory;

    private final Map<String, CoreSession> sessions = new ConcurrentHashMap<String, CoreSession>();

    private final Map<String, DocumentType> docTypes = new Hashtable<String, DocumentType>();

    private final Map<String, DocumentProvider> documentProviders = new Hashtable<String, DocumentProvider>();

    // hiding the default constructor from clients
    protected CoreInstance() {
    }

    /**
     * Gets the CoreInstance singleton.
     *
     * @return the server instance
     */
    public static CoreInstance getInstance() {
        return instance;
    }

    public DocumentType getCachedDocumentType(String type) {
        return docTypes.get(type);
    }

    public void cacheDocumentType(DocumentType docType) {
        docTypes.put(docType.getName(), docType);
    }

    public CoreSession open(String repositoryName,
            Map<String, Serializable> context) throws ClientException {
        // instantiate a new client
        try {
            RepositoryManager rm = Framework.getService(RepositoryManager.class);
            CoreSession session = null;
            if (rm != null) {
                Repository repo = rm.getRepository(repositoryName);
                if (repo == null) {
                    throw new ClientException("No such repository: "
                            + repositoryName);
                }
                // connect to the server
                session = repo.open(context);
            }
            // ------ FIXME only for compat with tests ---
            if (session == null) {
                session = compatOpen(repositoryName, context);
            }
            // --------------------------------------------------------
            return session;
        } catch (Exception e) {
            throw new ClientException(
                    "Failed to intialize core session on repository "
                            + repositoryName, e);
        }
    }

    /**
     * Obsolete method only for compatibility with existing tests. Should be
     * removed.
     *
     * @deprecated remove it
     */
    @Deprecated
    private CoreSession compatOpen(String repositoryName,
            Map<String, Serializable> context) throws ClientException {
        // instantiate a new client
        CoreSession client = factory.getSession();
        // connect to the server
        client.connect(repositoryName, context);
        // register the client locally
        sessions.put(client.getSessionId(), client);
        return client;
    }

    public void registerSession(String sid, CoreSession session) {
        sessions.put(sid, session);
    }

    public CoreSession unregisterSession(String sid) {
        return sessions.remove(sid);
    }

    public void close(CoreSession client) {
        String sid = client.getSessionId();
        if (sid == null) {
            return; // session not yet connected
        }
        client = sessions.remove(sid);
        if (client != null) {
            client.destroy();
        } else {
            log.warn("Trying to close a non referenced CoreSession (destroy method won't be called)");
        }
    }

    public boolean isSessionStarted(String sid) {
        return sessions.containsKey(sid);
    }

    /** @deprecated unused */
    @Deprecated
    public CoreSession[] getSessions() {
        Collection<CoreSession> valuesOfMap = sessions.values();
        return valuesOfMap.toArray(new CoreSession[0]);
    }

    /**
     * Gets the client bound to the given session.
     *
     * @param sid the session id
     * @return the client
     */
    public CoreSession getSession(String sid) {
        return sessions.get(sid);
    }

    public void initialize(CoreSessionFactory factory) {
        // TODO: to be able to test more easily with a variety of client
        // factories
        // if (instance.factory == null) {
        instance.factory = factory;
        // }
    }

    public CoreSessionFactory getFactory() {
        return factory;
    }

    public DocumentProvider getDocumentProvider(String sid) {
        DocumentProvider documentProvider = documentProviders.get(sid);
        if (documentProvider == null) {
            CoreSession session = getSession(sid);
            if (session != null) {
                documentProvider = Framework.getLocalService(DocumentProvider.class);
                if (documentProvider instanceof DocumentProviderManager) {
                    ((DocumentProviderManager) documentProvider).setSession(session);
                }
                documentProviders.put(sid, documentProvider);
            }
        }
        return documentProvider;
    }

}
