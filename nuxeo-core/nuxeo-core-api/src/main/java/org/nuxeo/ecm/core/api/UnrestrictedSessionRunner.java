/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.api;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
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

    protected CoreSession session;

    protected final boolean sessionIsAlreadyUnrestricted;

    protected final String repositoryName;

    /** True if a call to {@link #runUnrestricted} is in progress. */
    public boolean isUnrestricted;

    /**
     * Constructs a {@link UnrestrictedSessionRunner} given an existing session
     * (which may or may not be already unrestricted).
     *
     * @param session the available session
     */
    public UnrestrictedSessionRunner(CoreSession session) {
        this.session = session;
        sessionIsAlreadyUnrestricted = isUnrestricted(session);
        if (sessionIsAlreadyUnrestricted) {
            repositoryName = null;
        } else {
            repositoryName = session.getRepositoryName();
        }
    }

    /**
     * Constructs a {@link UnrestrictedSessionRunner} given a repository name.
     *
     * @param repositoryName the repository name
     */
    public UnrestrictedSessionRunner(String repositoryName) {
        session = null;
        sessionIsAlreadyUnrestricted = false;
        this.repositoryName = repositoryName;
    }

    protected boolean isUnrestricted(CoreSession session) {
        return "system".equals(session.getPrincipal().getName());
    }

    /**
     * Calls the {@link #run()} method with an unrestricted {@link #session}.
     * During this call, {@link #isUnrestricted} is set to {@code true}.
     *
     * @throws ClientException
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
                loginContext = Framework.login();
            } catch (LoginException e) {
                throw new ClientException(e);
            }
            try {
                Repository repository;
                CoreSession baseSession = session;
                try {
                    repository = Framework.getService(RepositoryManager.class).getRepository(
                            repositoryName);
                    if (repository == null) {
                        throw new ClientException("Cannot get repository: " +
                                repositoryName);
                    }
                    session = repository.open();
                } catch (ClientException e) {
                    throw e;
                } catch (Exception e) {
                    throw new ClientException(e);
                }
                try {
                    run();
                } finally {
                    try {
                        repository.close(session);
                    } catch (Exception e) {
                        throw new ClientException(e);
                    } finally {
                        session = baseSession;
                    }
                }
            } finally {
                try {
                    // loginContext may be null in tests
                    if (loginContext!=null) {
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
     * available will be the one passed to {@code
     * #UnrestrictedSessionRunner(CoreSession)}.
     *
     * @throws ClientException
     */
    public abstract void run() throws ClientException;

}
