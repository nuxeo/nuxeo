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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.repository;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.WrappedException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RepositoryInstanceHandler implements InvocationHandler {

    private final Repository repository;
    private final RepositoryExceptionHandler exceptionHandler;

    private CoreSession session;


    public RepositoryInstanceHandler(Repository repository, RepositoryExceptionHandler exceptionHandler) {
        this.repository = repository;
        this.exceptionHandler = exceptionHandler;
    }

    public RepositoryInstanceHandler(Repository repository) {
        this(repository, null);
    }

    /**
     * @return the exceptionHandler.
     */
    public RepositoryExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    private void rethrownException(Throwable t) throws Exception {
        if (t instanceof Exception) {
            throw (Exception)t;
        } else if (t instanceof Error) {
            throw (Error)t;
        } else {
            throw WrappedException.wrap(t);
        }
    }

    /**
     * @return the session.
     */
    public CoreSession getSession() throws Exception {
        if (session == null) {
            synchronized (this) {
                if (session == null) {
                    try {
                        session = repository.open();
                    } catch (Throwable t) {
                        if (exceptionHandler != null) {
                            session = exceptionHandler.handleAuthenticationFailure(repository, t);
                        } else {
                            rethrownException(t);
                        }
                    }
                }
            }
        }
        return session;
    }

    public void closeSession() throws Exception {
        if (session != null) {
            synchronized (this) {
                if (session != null) {
                    try {
                        CoreInstance.getInstance().close(session);
                    } catch (Throwable t) {
                        if (exceptionHandler != null) {
                            exceptionHandler.handleException(t);
                        } else {
                            rethrownException(t);
                        }
                    } finally {
                        session = null;
                    }
                }
            }
        }
    }

    @SuppressWarnings({"ObjectEquality"})
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        if (method.getDeclaringClass() == CoreSession.class) {
            try {
                return method.invoke(getSession(), args);
            } catch (Throwable t) {
                if (exceptionHandler != null) {
                    exceptionHandler.handleException(t);
                } else {
                    rethrownException(t);
                }
            }
        } else {
            //optimize matching by testing only one character
            String name = method.getName();
            char ch3 = name.charAt(3);
            switch (ch3) {
            case 'S': // getSession
                return getSession();
            case 'R': // getRepository
                return repository;
            case 's': // close
                closeSession();
                return null;
            }
        }
        throw new NoSuchMethodException("Should be a bug");
    }

}
