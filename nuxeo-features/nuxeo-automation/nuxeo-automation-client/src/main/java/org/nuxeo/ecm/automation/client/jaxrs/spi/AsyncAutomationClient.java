/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs.spi;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class AsyncAutomationClient extends AbstractAutomationClient {

    private static final Log log = LogFactory.getLog(AsyncAutomationClient.class);

    protected ExecutorService async;

    /**
     * Timeout in milliseconds for the wait of the asynchronous thread pool
     * termination. Default value: 2 seconds.
     */
    protected long asyncAwaitTerminationTimeout = 2000;

    /**
     * Instantiates a new asynchronous automation client with the default
     * timeout for the wait of the asynchronous thread pool termination: 2
     * seconds.
     */
    public AsyncAutomationClient(String url) {
        this(url, Executors.newCachedThreadPool(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                return new Thread("AutomationAsyncExecutor");
            }
        }));
    }

    /**
     * Instantiates a new asynchronous automation client with the given
     * asynchronous executor and the default timeout for the wait of the
     * asynchronous thread pool termination: 2 seconds.
     */
    public AsyncAutomationClient(String url, ExecutorService executor) {
        super(url);
        async = executor;
    }

    /**
     * Instantiates a new asynchronous automation client with the given timeout
     * in milliseconds for the wait of the asynchronous thread pool termination.
     *
     * @since 5.7
     */
    public AsyncAutomationClient(String url, long asyncAwaitTerminationTimeout) {
        this(url);
        this.asyncAwaitTerminationTimeout = asyncAwaitTerminationTimeout;
    }

    /**
     * Instantiates a new asynchronous automation client with the given
     * asynchronous executor and the given timeout in milliseconds for the wait
     * of the asynchronous thread pool termination.
     *
     * @since 5.7
     */
    public AsyncAutomationClient(String url, ExecutorService executor,
            long asyncAwaitTerminationTimeout) {
        this(url, executor);
        this.asyncAwaitTerminationTimeout = asyncAwaitTerminationTimeout;
    }

    @Override
    public void asyncExec(Runnable runnable) {
        async.execute(runnable);
    }

    @Override
    public synchronized void shutdown() {
        try {
            async.awaitTermination(asyncAwaitTerminationTimeout,
                    TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error(e, e);
        }
        super.shutdown();
        async = null;
    }

}
