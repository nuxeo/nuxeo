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

package org.nuxeo.ecm.core.api;

import java.security.Principal;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper class to run code with an unrestricted session.
 * <p>
 * The caller must implement the {@link #run} method, and call
 * {@link #runUnrestricted}.
 *
 * @author Florent Guillaume
 */
public abstract class UnrestrictedSessionRunner {

    protected String originatingUsername;

    protected CoreSession session;

    protected final boolean sessionIsAlreadyUnrestricted;

    protected final String repositoryName;

    /** True if a call to {@link #runUnrestricted} is in progress. */
    public boolean isUnrestricted;

    /**
     * Constructs a {@link UnrestrictedSessionRunner} given an existing session
     * (which may or may not be already unrestricted).
     * <p>
     * Originating user is taken on given session.
     *
     * @param session the available session
     */
    protected UnrestrictedSessionRunner(CoreSession session) {
        this.session = session;
        sessionIsAlreadyUnrestricted = isUnrestricted(session);
        if (sessionIsAlreadyUnrestricted) {
            repositoryName = null;
        } else {
            repositoryName = session.getRepositoryName();
        }
        Principal pal = session.getPrincipal();
        if (pal != null) {
            originatingUsername = pal.getName();
        }
    }

    /**
     * Constructs a {@link UnrestrictedSessionRunner} given a repository name.
     *
     * @param repositoryName the repository name
     */
    protected UnrestrictedSessionRunner(String repositoryName) {
        session = null;
        sessionIsAlreadyUnrestricted = false;
        this.repositoryName = repositoryName;
    }

    /**
     * Constructs a {@link UnrestrictedSessionRunner} given a repository name
     * and an originating user name.
     *
     * @param repositoryName the repository name
     * @param originatingUser the user name behind the system user
     */
    protected UnrestrictedSessionRunner(String repositoryName,
            String originatingUser) {
        session = null;
        sessionIsAlreadyUnrestricted = false;
        this.repositoryName = repositoryName;
        this.originatingUsername = originatingUser;
    }

    public String getOriginatingUsername() {
        return originatingUsername;
    }

    public void setOriginatingUsername(String originatingUsername) {
        this.originatingUsername = originatingUsername;
    }

    protected boolean isUnrestricted(CoreSession session) {
        return SecurityConstants.SYSTEM_USERNAME.equals(session.getPrincipal().getName())
                || (session.getPrincipal() instanceof NuxeoPrincipal && ((NuxeoPrincipal) session.getPrincipal()).isAdministrator());
    }

    /**
     * Calls the {@link #run()} method with an unrestricted {@link #session}.
     * During this call, {@link #isUnrestricted} is set to {@code true}.
     */
    public void runUnrestricted() throws ClientException {
        isUnrestricted = true;
        try {
            if (sessionIsAlreadyUnrestricted) {
                run();
                return;
            }

            LoginContext loginContext;
            try {
                loginContext = Framework.loginAs(originatingUsername);
            } catch (LoginException e) {
                throw new ClientException(e);
            }
            try {
                CoreSession baseSession = session;
                if (baseSession != null
                        && !baseSession.isStateSharedByAllThreadSessions()) {
                    // save base session state for unrestricted one
                    baseSession.save();
                }
                try {
                    Repository repository = Framework.getService(
                            RepositoryManager.class).getRepository(
                            repositoryName);
                    if (repository == null) {
                        throw new ClientException("Cannot get repository: "
                                + repositoryName);
                    }
                    session = repository.open();
                    if (loginContext == null && Framework.isTestModeSet()) {
                        NuxeoPrincipal principal = (NuxeoPrincipal) session.getPrincipal();
                        if (principal instanceof SystemPrincipal) {
                            // we are in a test that is not using authentication =>
                            // we're not stacking the originating user in the authentication stack
                            // so we're setting manually now
                            principal.setOriginatingUser(originatingUsername);
                        }
                    }
                } catch (ClientException e) {
                    throw e;
                } catch (Exception e) {
                    throw new ClientException(e);
                }
                try {
                    run();
                } finally {
                    try {
                        if (!session.isStateSharedByAllThreadSessions()) {
                            // save unrestricted state for base session
                            session.save();
                        }
                        Repository.close(session);
                    } catch (Exception e) {
                        throw new ClientException(e);
                    } finally {
                        if (baseSession != null
                                && !baseSession.isStateSharedByAllThreadSessions()) {
                            // process invalidations from unrestricted session
                            baseSession.save();
                        }
                        session = baseSession;
                    }
                }
            } finally {
                try {
                    // loginContext may be null in tests
                    if (loginContext != null) {
                        loginContext.logout();
                    }
                } catch (LoginException e) {
                    throw new ClientException(e);
                }
            }
        } finally {
            isUnrestricted = false;
        }
    }

    /**
     * This method will be called by {@link #runUnrestricted()} with
     * {@link #session} available as an unrestricted session.
     * <p>
     * It can also be called directly in which case the {@link #session}
     * available will be the one passed to
     * {@code #UnrestrictedSessionRunner(CoreSession)}.
     */
    public abstract void run() throws ClientException;

}
