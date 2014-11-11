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
package org.nuxeo.ecm.core.storage.sql.net;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;

import org.nuxeo.ecm.core.api.WrappedException;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.CachingRowMapper;
import org.nuxeo.ecm.core.storage.sql.InvalidationsQueue;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.ecm.core.storage.sql.Repository;
import org.nuxeo.ecm.core.storage.sql.Session;

/**
 * Will execute in a separate thread the commands passed through {@link #call}.
 */
public class MapperInvoker extends Thread {

    private static final String INVOKER_INIT = "__init";

    private static final String INVOKER_CLOSE = "__close";

    private static final Map<String, Method> mapperMethods = new HashMap<String, Method>();
    static {
        for (Method m : Mapper.class.getMethods()) {
            mapperMethods.put(m.getName(), m);
        }
    }

    protected static final class MethodCall {
        public final String methodName;

        public final Object[] args;

        protected final CountDownLatch resultReady;

        protected Object result;

        public MethodCall(String methodName, Object[] args) {
            this.methodName = methodName;
            this.args = args;
            this.resultReady = new CountDownLatch(1);
        }

        public void setResult(Object result) {
            this.result = result;
            resultReady.countDown();
        }

        public Object getResult() throws InterruptedException {
            resultReady.await();
            return result;
        }
    }

    private final Repository repository;

    private final InvalidationsQueue eventQueue;

    private Session session;

    private Mapper mapper;

    protected MapperClientInfo clientInfo;

    protected final SynchronousQueue<MethodCall> methodCalls;

    public MapperInvoker(Repository repository, String name,
            InvalidationsQueue eventQueue, MapperClientInfo info)
            throws Throwable {
        super(name);
        this.repository = repository;
        this.eventQueue = eventQueue;
        this.methodCalls = new SynchronousQueue<MethodCall>();
        this.clientInfo = info;
    }

    // called in the main thread
    public void init() throws Throwable {
        call(INVOKER_INIT);
    }

    // called in the main thread
    public void close() throws Throwable {
        try {
            call(INVOKER_CLOSE);
        } finally {
            interrupt();
            join();
        }
    }

    // called in the main thread
    public Object call(String methodName, Object... args) throws Throwable {
        MethodCall call = new MethodCall(methodName, args);
        methodCalls.put(call);
        return call.getResult();
    }

    @Override
    public void run() {
        try {
            while (true) {
                MethodCall call = methodCalls.take();
                Object res;
                try {
                    res = localCall(call.methodName, call.args);
                } catch (InvocationTargetException e) {
                    res = WrappedException.wrap(e.getCause());
                } catch (Exception e) {
                    // wrap the exception as its class may not be present on the
                    // server, or be slightly different
                    // (javax.resource.ResourceException)
                    res = WrappedException.wrap(e);
                }
                call.setResult(res);
            }
        } catch (InterruptedException e) {
            // end
        }
    }

    protected Object localCall(String methodName, Object[] args)
            throws Exception {
        if (INVOKER_INIT.equals(methodName)) {
            session = repository.getConnection();
            mapper = session.getMapper();
            // replace event queue with the client-repo specific one
            ((CachingRowMapper) mapper).setEventQueue(eventQueue);
            return null;
        } else if (INVOKER_CLOSE.equals(methodName)) {
            session.close();
            mapper = null;
            return null;
        } else if (Mapper.CLOSE.equals(methodName)) {
            // ignored, done by above invoker close, on the session
            // (we must not close the mapper directly as it may be in a pool)
            return null;
        }
        Method method = mapperMethods.get(methodName);
        if (method == null) {
            throw new StorageException("Unknown Mapper method: " + methodName);
        } else {
            return method.invoke(mapper, args);
        }
    }

}
