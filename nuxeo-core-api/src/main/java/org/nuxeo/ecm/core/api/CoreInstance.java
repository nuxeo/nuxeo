/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * The CoreInstance is the main access point to a CoreSession.
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
 * <pre>
 * CoreSession coreSession = CoreInstance.getInstance().open(&quot;demo&quot;, null);
 * DocumentModel root = client.getRootDocument();
 * // ... do something in that session ...
 * CoreInstance.getInstance().close(client);
 * </pre>
 */
public class CoreInstance {

    private static final Log log = LogFactory.getLog(CoreInstance.class);

    private static final CoreInstance instance = new CoreInstance();

    public static class RegistrationInfo extends Throwable {
        private static final long serialVersionUID = 1L;

        public final CoreSession session;

        public final String threadName;

        RegistrationInfo(CoreSession session) {
            super("Session registration context (" + session.getSessionId()
                    + "," + Thread.currentThread().getName() + ")");
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

    /**
     * Opens a {@link CoreSession} for the currently logged-in user.
     * <p>
     * The session must be closed using {@link CoreSession#close}.
     *
     * @param repositoryName the repository name, or {@code null} for the
     *            default repository
     * @return the session
     * @since 5.9.3
     */
    public static CoreSession openCoreSession(String repositoryName)
            throws ClientException {
        return openCoreSession(repositoryName, (Map<String, Serializable>) null);
    }

    /**
     * Opens a {@link CoreSession} for the given user.
     * <p>
     * The session must be closed using {@link CoreSession#close}.
     *
     * @param repositoryName the repository name, or {@code null} for the
     *            default repository
     * @param username the user name
     * @return the session
     * @since 5.9.3
     */
    public static CoreSession openCoreSession(String repositoryName,
            String username) throws ClientException {
        Map<String, Serializable> context = new HashMap<String, Serializable>();
        context.put("username", username);
        return openCoreSession(repositoryName, context);
    }

    /**
     * Opens a {@link CoreSession} for the given principal.
     * <p>
     * The session must be closed using {@link CoreSession#close}.
     *
     * @param repositoryName the repository name, or {@code null} for the
     *            default repository
     * @param principal the principal
     * @return the session
     * @since 5.9.3
     */
    public static CoreSession openCoreSession(String repositoryName,
            Principal principal) throws ClientException {
        Map<String, Serializable> context = new HashMap<String, Serializable>();
        if (principal instanceof NuxeoPrincipal) {
            context.put("principal", (NuxeoPrincipal) principal);
        } else {
            context.put("username", principal.getName());
        }
        return openCoreSession(repositoryName, context);
    }

    /**
     * Opens a {@link CoreSession} for a system user.
     * <p>
     * The session must be closed using {@link CoreSession#close}.
     *
     * @param repositoryName the repository name, or {@code null} for the
     *            default repository
     * @return the session
     * @since 5.9.3
     */
    public static CoreSession openCoreSessionSystem(String repositoryName)
            throws ClientException {
        return openCoreSession(repositoryName,
                SecurityConstants.SYSTEM_USERNAME);
    }

    /**
     * @deprecated since 5.9.3, use {@link #openCoreSession} instead.
     */
    @Deprecated
    public CoreSession open(String repositoryName,
            Map<String, Serializable> context) throws ClientException {
        return openCoreSession(repositoryName, context);
    }

    /**
     * NOT PUBLIC, DO NOT CALL. Kept public for compatibility with old code.
     * <p>
     * Opens a {@link CoreSession} for the given context.
     *
     * @param repositoryName the repository name, or {@code null} for the
     *            default repository
     * @param context the session open context
     * @return the session
     */
    public static CoreSession openCoreSession(String repositoryName,
            Map<String, Serializable> context) throws ClientException {
        RepositoryManager repositoryManager = Framework.getLocalService(RepositoryManager.class);
        Repository repository;
        if (repositoryName == null) {
            repository = repositoryManager.getDefaultRepository();
        } else {
            repository = repositoryManager.getRepository(repositoryName);
        }
        if (repository == null) {
            throw new ClientException("No such repository: " + repositoryName);
        }
        try {
            return repository.open(context);
        } catch (Exception e) {
            throw new ClientException(e.getMessage(), e);
        }
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
        // careful about new sessions appearing while we iterate
        Collection<RegistrationInfo> infos = sessions.values();
        List<CoreSession> list = new ArrayList<CoreSession>(infos.size());
        for (RegistrationInfo ri : infos) {
            list.add(ri.session);
        }
        return list.toArray(new CoreSession[0]);
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
        return sessions.get(sid).session;
    }

    public RegistrationInfo getSessionRegistrationInfo(String sid) {
        return sessions.get(sid);
    }

}
