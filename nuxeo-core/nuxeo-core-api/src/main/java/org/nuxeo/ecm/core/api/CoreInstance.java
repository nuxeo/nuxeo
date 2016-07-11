/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.nuxeo.ecm.core.api.CoreSessionService.CoreSessionRegistrationInfo;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.api.local.LoginStack;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.LoginComponent;

/**
 * The CoreInstance is the main access point to a CoreSession.
 */
public class CoreInstance {

    private static final CoreInstance INSTANCE = new CoreInstance();

    private CoreInstance() {
    }

    /**
     * Gets the CoreInstance singleton.
     *
     * @deprecated since 8.4, use {@link CoreSessionService} directly.
     */
    public static CoreInstance getInstance() {
        return INSTANCE;
    }

    /**
     * Opens a {@link CoreSession} for the currently logged-in user.
     * <p>
     * The session must be closed using {@link CoreSession#close}.
     *
     * @param repositoryName the repository name, or {@code null} for the default repository
     * @return the session
     * @since 5.9.3
     */
    public static CoreSession openCoreSession(String repositoryName) {
        return openCoreSession(repositoryName, getPrincipal((String) null));
    }

    /**
     * MUST ONLY BE USED IN UNIT TESTS to open a {@link CoreSession} for the given user.
     * <p>
     * The session must be closed using {@link CoreSession#close}.
     *
     * @param repositoryName the repository name, or {@code null} for the default repository
     * @param username the user name
     * @return the session
     * @since 5.9.3
     */
    public static CoreSession openCoreSession(String repositoryName, String username) {
        return openCoreSession(repositoryName, getPrincipal(username));
    }

    /**
     * Opens a {@link CoreSession} for a system user.
     * <p>
     * The session must be closed using {@link CoreSession#close}.
     *
     * @param repositoryName the repository name, or {@code null} for the default repository
     * @return the session
     * @since 5.9.3
     */
    public static CoreSession openCoreSessionSystem(String repositoryName) {
        return openCoreSession(repositoryName, getPrincipal((SecurityConstants.SYSTEM_USERNAME)));
    }

    /**
     * Opens a {@link CoreSession} for a system user with an optional originating username.
     * <p>
     * The session must be closed using {@link CoreSession#close}.
     *
     * @param repositoryName the repository name, or {@code null} for the default repository
     * @param originatingUsername the originating username to set on the SystemPrincipal
     * @return the session
     * @since 8.1
     */
    public static CoreSession openCoreSessionSystem(String repositoryName, String originatingUsername) {
        NuxeoPrincipal principal = getPrincipal((SecurityConstants.SYSTEM_USERNAME));
        principal.setOriginatingUser(originatingUsername);
        return openCoreSession(repositoryName, principal);
    }

    /**
     * @deprecated since 5.9.3, use {@link #openCoreSession} instead.
     */
    @Deprecated
    public CoreSession open(String repositoryName, Map<String, Serializable> context) {
        return openCoreSession(repositoryName, getPrincipal(context));
    }

    /**
     * NOT PUBLIC, DO NOT CALL. Kept public for compatibility with old code.
     * <p>
     * Opens a {@link CoreSession} for the given context.
     *
     * @param repositoryName the repository name, or {@code null} for the default repository
     * @param context the session open context
     * @return the session
     */
    public static CoreSession openCoreSession(String repositoryName, Map<String, Serializable> context) {
        return openCoreSession(repositoryName, getPrincipal(context));
    }

    /**
     * MUST ONLY BE USED IN UNIT TESTS to open a {@link CoreSession} for the given principal.
     * <p>
     * The session must be closed using {@link CoreSession#close}.
     *
     * @param repositoryName the repository name, or {@code null} for the default repository
     * @param principal the principal
     * @return the session
     * @since 5.9.3
     */
    public static CoreSession openCoreSession(String repositoryName, Principal principal) {
        if (principal instanceof NuxeoPrincipal) {
            return openCoreSession(repositoryName, (NuxeoPrincipal) principal);
        } else {
            return openCoreSession(repositoryName, getPrincipal(principal.getName()));
        }
    }

    /**
     * MUST ONLY BE USED IN UNIT TESTS to open a {@link CoreSession} for the given principal.
     * <p>
     * The session must be closed using {@link CoreSession#close}.
     *
     * @param repositoryName the repository name, or {@code null} for the default repository
     * @param principal the principal
     * @return the session
     * @since 5.9.3
     */
    public static CoreSession openCoreSession(String repositoryName, NuxeoPrincipal principal) {
        if (repositoryName == null) {
            RepositoryManager repositoryManager = Framework.getLocalService(RepositoryManager.class);
            repositoryName = repositoryManager.getDefaultRepository().getName();
        }
        return Framework.getService(CoreSessionService.class).createCoreSession(repositoryName, principal);
    }

    /**
     * Gets an existing open session for the given session id.
     * <p>
     * The returned CoreSession must not be closed, as it is owned by someone else.
     *
     * @param sessionId the session id
     * @return the session, which must not be closed
     */
    public CoreSession getSession(String sessionId) {
        return Framework.getService(CoreSessionService.class).getCoreSession(sessionId);
    }

    /**
     * Use {@link CoreSession#close} instead.
     *
     * @since 5.9.3
     */
    public static void closeCoreSession(CoreSession session) {
        Framework.getService(CoreSessionService.class).releaseCoreSession(session);
    }

    protected static NuxeoPrincipal getPrincipal(Map<String, Serializable> map) {
        if (map == null) {
            return getPrincipal((String) null); // logged-in principal
        }
        NuxeoPrincipal principal = (NuxeoPrincipal) map.get("principal");
        if (principal == null) {
            principal = getPrincipal((String) map.get("username"));
        }
        return principal;
    }

    protected static NuxeoPrincipal getPrincipal(String username) {
        if (username != null) {
            if (SYSTEM_USERNAME.equals(username)) {
                return new SystemPrincipal(null);
            } else {
                return new UserPrincipal(username, new ArrayList<String>(), false, false);
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
                    throw new RuntimeException("Unsupported principal: " + p.getClass());
                }
            } else {
                if (Framework.isTestModeSet()) {
                    return new SystemPrincipal(null);
                } else {
                    throw new NuxeoException("Cannot create a CoreSession outside a security context, "
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
     * @deprecated since 8.4, use {@link CoreSessionService#getNumberOfOpenSessions} directly
     */
    public int getNumberOfSessions() {
        return Framework.getService(CoreSessionService.class).getNumberOfOpenCoreSessions();
    }

    /**
     * Gets the number of open sessions.
     *
     * @since 5.4.2
     * @deprecated since 8.4, use {@link CoreSessionService#getRegistrationInfos} directly
     */
    public Collection<CoreSessionRegistrationInfo> getRegistrationInfos() {
        return Framework.getService(CoreSessionService.class).getCoreSessionRegistrationInfos();
    }

}
