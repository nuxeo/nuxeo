/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs.spi;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class AsyncAutomationClient extends AbstractAutomationClient {

    private static final Log log = LogFactory.getLog(AsyncAutomationClient.class);

    protected ExecutorService async;

    /**
     * Timeout in milliseconds for the wait of the asynchronous thread pool termination. Default value: 2 seconds.
     */
    protected long asyncAwaitTerminationTimeout = 2000;

    protected static ExecutorService getExecutorService() {
        return Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread("AutomationAsyncExecutor");
            }
        });
    }

    /**
     * Instantiates a new asynchronous automation client with the default timeout for the wait of the asynchronous
     * thread pool termination: 2 seconds.
     */
    public AsyncAutomationClient(String url) {
        this(url, getExecutorService());
    }

    /**
     * Instantiates a new asynchronous automation client with the default timeout for the wait of the asynchronous
     * thread pool termination: 2 seconds.
     *
     * @since 10.10
     */
    public AsyncAutomationClient(Supplier<String> urlSupplier) {
        this(urlSupplier, getExecutorService());
    }

    /**
     * Instantiates a new asynchronous automation client with the given asynchronous executor and the default timeout
     * for the wait of the asynchronous thread pool termination: 2 seconds.
     */
    public AsyncAutomationClient(String url, ExecutorService executor) {
        super(url);
        async = executor;
    }

    /**
     * Instantiates a new asynchronous automation client with the given asynchronous executor and the default timeout
     * for the wait of the asynchronous thread pool termination: 2 seconds.
     *
     * @since 10.10
     */
    public AsyncAutomationClient(Supplier<String> urlSupplier, ExecutorService executor) {
        super(urlSupplier);
        async = executor;
    }

    /**
     * Instantiates a new asynchronous automation client with the given timeout in milliseconds for the wait of the
     * asynchronous thread pool termination.
     *
     * @since 5.7
     */
    public AsyncAutomationClient(String url, long asyncAwaitTerminationTimeout) {
        this(url);
        this.asyncAwaitTerminationTimeout = asyncAwaitTerminationTimeout;
    }

    /**
     * Instantiates a new asynchronous automation client with the given asynchronous executor and the given timeout in
     * milliseconds for the wait of the asynchronous thread pool termination.
     *
     * @since 5.7
     */
    public AsyncAutomationClient(String url, ExecutorService executor, long asyncAwaitTerminationTimeout) {
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
            async.awaitTermination(asyncAwaitTerminationTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        super.shutdown();
        async = null;
    }

}
