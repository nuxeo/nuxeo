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

import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.api.local.LoginStack;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.LoginComponent;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

/**
 * The CoreInstance is the main access point to a CoreSession.
 */
public class CoreInstance {

    private static final CoreInstance INSTANCE = new CoreInstance();

    public static class RegistrationInfo extends Throwable {
        private static final long serialVersionUID = 1L;

        public final CoreSession session;

        public RegistrationInfo(CoreSession session) {
            super("RegistrationInfo(" + Thread.currentThread().getName() + ", "
                    + session.getSessionId() + ")");
            this.session = session;
        }

    }

    /**
     * All open CoreSessionInfo, keyed by session id.
     */
    private final Map<String, RegistrationInfo> sessions = new ConcurrentHashMap<String, RegistrationInfo>();

    private CoreInstance() {
    }

    /**
     * Gets the CoreInstance singleton.
     */
    public static CoreInstance getInstance() {
        return INSTANCE;
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
        return openCoreSession(repositoryName, getPrincipal((String) null));
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
        return openCoreSession(repositoryName, getPrincipal(username));
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
                getPrincipal((SecurityConstants.SYSTEM_USERNAME)));
    }

    /**
     * @deprecated since 5.9.3, use {@link #openCoreSession} instead.
     */
    @Deprecated
    public CoreSession open(String repositoryName,
            Map<String, Serializable> context) throws ClientException {
        return openCoreSession(repositoryName, getPrincipal(context));
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
        return openCoreSession(repositoryName, getPrincipal(context));
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
        if (principal instanceof NuxeoPrincipal) {
            return openCoreSession(repositoryName, (NuxeoPrincipal) principal);
        } else {
            return openCoreSession(repositoryName,
                    getPrincipal(principal.getName()));
        }
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
            NuxeoPrincipal principal) throws ClientException {
        if (repositoryName == null) {
            RepositoryManager repositoryManager = Framework.getLocalService(RepositoryManager.class);
            repositoryName = repositoryManager.getDefaultRepository().getName();
        }
        return getInstance().acquireCoreSession(repositoryName, principal);
    }

    protected CoreSession acquireCoreSession(String repositoryName,
            NuxeoPrincipal principal) throws ClientException {
        CoreSession session = Framework.getLocalService(CoreSession.class);
        session.connect(repositoryName, principal);
        sessions.put(session.getSessionId(), new RegistrationInfo(session));
        return session;
    }

    /**
     * Gets an existing open session for the given session id.
     * <p>
     * The returned CoreSession must not be closed, as it is owned by someone
     * else.
     *
     * @param sessionId the session id
     * @return the session, which must not be closed
     */
    public CoreSession getSession(String sessionId) {
        if (sessionId == null) {
            throw new NullPointerException("null sessionId");
        }
        RegistrationInfo csi = sessions.get(sessionId);
        return csi == null ? null : csi.session;
    }

    /**
     * Use {@link CoreSession#close} instead.
     *
     * @since 5.9.3
     */
    public static void closeCoreSession(CoreSession session) {
        getInstance().releaseCoreSession(session);
    }

    protected void releaseCoreSession(CoreSession session) {
        String sessionId = session.getSessionId();
        RegistrationInfo csi = sessions.remove(sessionId);
        if (csi == null) {
            throw new RuntimeException("Closing unknown CoreSession: "
                    + sessionId);
        }
        session.destroy();
    }

    protected static NuxeoPrincipal getPrincipal(Map<String, Serializable> map)
            throws ClientException {
        if (map == null) {
            return getPrincipal((String) null); // logged-in principal
        }
        NuxeoPrincipal principal = (NuxeoPrincipal) map.get("principal");
        if (principal == null) {
            principal = getPrincipal((String) map.get("username"));
        }
        return principal;
    }

    protected static NuxeoPrincipal getPrincipal(String username)
            throws ClientException {
        if (username != null) {
            if (SYSTEM_USERNAME.equals(username)) {
                return new SystemPrincipal(null);
            } else {
                return new UserPrincipal(username, new ArrayList<String>(),
                        false, false);
            }
        } else {
            LoginStack.Entry entry = ClientLoginModule.getCurrentLogin();
            if (entry != null) {
                Principal p = entry.getPrincipal();
                if (p instanceof NuxeoPrincipal) {
                    return (NuxeoPrincipal) p;
                } else if (LoginComponent.isSystemLogin(p)) {
                    return new SystemPrincipal(p.getName());
                } else {
                    throw new RuntimeException("Unsupported principal: "
                            + p.getClass());
                }
            } else {
                if (Framework.isTestModeSet()) {
                    return new SystemPrincipal(null);
                } else {
                    throw new ClientException(
                            "Cannot create a CoreSession outside a security context, "
                                    + " login() missing.");
                }
            }
        }
    }

    /**
     * @deprecated since 5.9.3, use {@link CoreSession#close} instead.
     */
    @Deprecated
    public void close(CoreSession session) {
        session.close(); // calls back closeCoreSession
    }

    /**
     * Gets the number of open sessions.
     *
     * @since 5.4.2
     */
    public int getNumberOfSessions() {
        return sessions.size();
    }

    public Collection<RegistrationInfo> getRegistrationInfos() {
        return sessions.values();
    }

    public Collection<RegistrationInfo> getRegistrationInfosLive(final boolean onThread) {
        return Collections2.filter(sessions.values(), new Predicate<RegistrationInfo>() {

            @Override
            public boolean apply(RegistrationInfo input) {
                return input.session.isLive(onThread);
            }

        });
    }

    public void cleanupThisThread() throws ClientException {
        ClientException errors = new ClientException("disconnecting from storage for you");
        for (RegistrationInfo each:CoreInstance.getInstance().getRegistrationInfosLive(true)) {
            each.session.destroy();
            errors.addSuppressed(each);
        }
        if (errors.getSuppressed().length > 0) {
            throw errors;
        }
    }
}
