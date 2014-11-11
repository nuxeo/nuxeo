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

package org.nuxeo.ecm.core.client;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.WrappedException;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryInstance;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RepositoryInstanceHandler implements InvocationHandler {

    private final Repository repository;
    private CoreSession session;
    private final RepositoryExceptionHandler exceptionHandler;
    private RepositoryInstance  proxy;


    public RepositoryInstanceHandler(Repository repository) {
        this(repository, null);
    }

    public RepositoryInstanceHandler(Repository repository, RepositoryExceptionHandler exceptionHandler) {
        this.repository = repository;
        this.exceptionHandler = exceptionHandler;
    }

    public RepositoryExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    public RepositoryInstance  getProxy() {
        if (proxy == null) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = Repository.class.getClassLoader();
            }
            proxy = (RepositoryInstance)Proxy.newProxyInstance(cl,
                    new Class[] { RepositoryInstance.class },
                    this);
        }
        return proxy;
    }

    private static void rethrowException(Throwable t) throws Exception {
        if (t instanceof Exception) {
            throw (Exception) t;
        } else if (t instanceof Error) {
            throw (Error) t;
        } else {
            throw WrappedException.wrap(t);
        }
    }

    public CoreSession getSession() throws Exception {
        if (session == null) {
            synchronized (this) {
                if (session == null) {
                    try {
                        open(repository);
                    } catch (Throwable t) {
                        if (exceptionHandler != null) {
                            session = exceptionHandler.handleAuthenticationFailure(repository, t);
                        } else {
                            rethrowException(t);
                        }
                    }
                }
            }
        }
        return session;
    }

    private void open(Repository repository) throws Exception {
        session = Framework.getService(CoreSession.class, repository.getName());
        String repositoryUri = repository.getRepositoryUri();
        if (repositoryUri == null) {
            repositoryUri = repository.getName();
        }
        String sid = session.connect(repositoryUri, new HashMap<String, Serializable>());
        // register session on local JVM so it can be used later by doc models
        CoreInstance.getInstance().registerSession(sid, proxy);
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
                            rethrowException(t);
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
                    rethrowException(t);
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
