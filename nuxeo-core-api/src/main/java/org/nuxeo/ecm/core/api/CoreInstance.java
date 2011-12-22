/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
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
 * CoreSessionFactory objects are implementation-dependent and may be
 * registered using extension points. See {@link CoreSessionFactory} for more
 * details.
 * <p>
 * Thus you can use a different implementation for the local ServerConnector
 * than the one for the remote ServerConnector.
 * <p>
 * When clients need to perform a connection to a repository, they simply open
 * a new session using the {@link CoreInstance#open(String, Map)} method.
 * <p>
 * When the client has done its work it <b>must</b> close its session by
 * calling {@link CoreInstance#close(CoreSession)}.
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

    public static class RegistrationInfo extends Throwable {
        private static final long serialVersionUID = 1L;
        public final CoreSession session;
        public final String threadName;
        RegistrationInfo(CoreSession session) {
            super("Session registration context (" + session.getSessionId() + "," + Thread.currentThread().getName() + ")");
            this.session = session;
            this.threadName = Thread.currentThread().getName();
        }
    }

    private final Map<String, RegistrationInfo> sessions = new ConcurrentHashMap<String, RegistrationInfo>();

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
        registerSession(client.getSessionId(), client);
        return client;
    }

    public void registerSession(String sid, CoreSession session) {
        if (log.isDebugEnabled()) {
            log.debug("Register session with id '" + sid + "'.");
        }
        sessions.put(sid, new RegistrationInfo(session));
    }

    public CoreSession unregisterSession(String sid) {
        if (log.isDebugEnabled()) {
            log.debug("Unregister session with id '" + sid + "'.");
        }
        RegistrationInfo info = sessions.remove(sid);
        if (info == null) {
            return null;
        }
        return info.session;
    }

    public void close(CoreSession client) {
        String sid = client.getSessionId();
        if (sid == null) {
            return; // session not yet connected
        }
        client = unregisterSession(sid);
        if (client != null) {
            client.destroy();
        } else {
            log.warn("Trying to close a non referenced CoreSession (destroy method won't be called)");
        }
    }

    public boolean isSessionStarted(String sid) {
        return sessions.containsKey(sid);
    }

    /**
     * Returns the number of registered sessions.
     *
     * @since 5.4.2
     */
    public int getNumberOfSessions() {
        return sessions.size();
    }

    public CoreSession[] getSessions() {
        Collection<RegistrationInfo> infos = sessions.values();
        CoreSession[] ret = new CoreSession[infos.size()];
       Iterator<RegistrationInfo> it = infos.iterator();
       int i = 0;
       while (it.hasNext()) {
           ret[i++] = it.next().session;
       }
       return ret;
    }

    public Collection<RegistrationInfo> getRegistrationInfos() {
        return sessions.values();
    }
    /**
     * Gets the client bound to the given session.
     *
     * @param sid the session id
     * @return the client
     */
    public CoreSession getSession(String sid) {
        HashMap<String, CoreSession> reentrantSession = DocumentModelImpl.reentrantCoreSession.get();
        if (reentrantSession != null && reentrantSession.containsKey(sid)) {
            return reentrantSession.get(sid);
        }
        return sessions.get(sid).session;
    }

    public RegistrationInfo getSessionRegistrationInfo(String sid) {
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

}
