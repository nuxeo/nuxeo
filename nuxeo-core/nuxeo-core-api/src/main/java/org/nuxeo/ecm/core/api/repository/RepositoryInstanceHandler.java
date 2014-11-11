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

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.WrappedException;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RepositoryInstanceHandler implements InvocationHandler, RepositoryConnection, Serializable {

    public static final Object NULL = new Object();

    protected static final ConcurrentHashMap<Method, Method> methods = new ConcurrentHashMap<Method, Method>();
    protected static ConcurrentHashMap<Method, MethodInvoker> invokers = new ConcurrentHashMap<Method, MethodInvoker>();

    protected final transient Repository repository;
    protected final transient RepositoryExceptionHandler exceptionHandler;
    protected transient CoreSession session;
    protected transient RepositoryInstance  proxy;

    public RepositoryInstanceHandler(Repository repository, RepositoryExceptionHandler exceptionHandler) {
        this.repository = repository;
        this.exceptionHandler = exceptionHandler;
    }

    public RepositoryInstanceHandler(Repository repository) {
        this(repository, null);
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
                    getProxyInterfaces(),
                    this);
        }
        return proxy;
    }

    public Class<?>[] getProxyInterfaces() {
        return new Class[] { RepositoryInstance.class };
    }

    protected static void rethrowException(Throwable t) throws Exception {
        if (t instanceof Exception) {
            throw (Exception) t;
        } else if (t instanceof Error) {
            throw (Error) t;
        } else {
            throw WrappedException.wrap(t);
        }
    }

    public Repository getRepository() {
        return repository;
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

    protected void open(Repository repository) throws Exception {
        session = Framework.getService(CoreSession.class, repository.getName());
        String repositoryUri = repository.getRepositoryUri();
        if (repositoryUri == null) {
            repositoryUri = repository.getName();
        }
        String sid = session.connect(repositoryUri, new HashMap<String, Serializable>());
        // register session on local JVM so it can be used later by doc models
        CoreInstance.getInstance().registerSession(sid, proxy);
    }

    public void close() throws Exception {
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
        try {
//            MethodInvoker invoker = invokers.get(method);
//          if (invoker != null) {
//          return invoker.invoke(this, method, args);
//          } else if (method.getDeclaringClass() == CoreSession.class) {
//          return method.invoke(getSession(), args);
//          } else {
//          return method.invoke(this, args);
//          }
            if (method.getDeclaringClass() == CoreSession.class) {
                Method m = methods.get(method); // check if method was overwritten
                if (m == null) {
                    try {
                        m = getClass().getMethod(method.getName(), method.getParameterTypes());
                    } catch (NoSuchMethodException e) {
                        m = method;
                    }
                    methods.put(method, m);
                }
                return m.invoke(m == method ? getSession() : this, args);
            }
            return method.invoke(this, args);
        } catch (InvocationTargetException e) {
            // throw a ClientException
            Throwable cause = e.getCause();
            if (exceptionHandler != null) {
                exceptionHandler.handleException(cause);
            }
            throw cause;
        } catch (Throwable t) {
            if (exceptionHandler != null) {
                exceptionHandler.handleException(t);
            }
            throw t;
        }
    }

    protected Object getImpl() {
        return this;
    }

    public Object writeReplace() throws ObjectStreamException {
        return Proxy.getInvocationHandler(session);
    }

}
