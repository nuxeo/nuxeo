/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.core.event.impl;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.ReconnectedEventBundle;
import org.nuxeo.ecm.core.event.tx.EventBundleTransactionHandler;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author tiry
 */
public class AsyncEventExecutor {

    private static final Log log = LogFactory.getLog(AsyncEventExecutor.class);

    public static final int QUEUE_SIZE = Integer.MAX_VALUE;

    protected final ThreadPoolExecutor executor;

    protected final BlockingQueue<Runnable> queue;


    public static AsyncEventExecutor create() {
        String val = Framework.getProperty("org.nuxeo.ecm.core.event.async.poolSize");
        int poolSize = val == null ? 4 : Integer.parseInt(val);
        val = Framework.getProperty("org.nuxeo.ecm.core.event.async.maxPoolSize");
        int maxPoolSize = val == null ? 16 : Integer.parseInt(val);
        val = Framework.getProperty("org.nuxeo.ecm.core.event.async.keepAliveTime");
        int keepAliveTime = val == null ? 4 : Integer.parseInt(val);
        val = Framework.getProperty("org.nuxeo.ecm.core.event.async.queueSize");
        int queueSize = val == null ? QUEUE_SIZE : Integer.parseInt(val);
        return new AsyncEventExecutor(poolSize, maxPoolSize, keepAliveTime, queueSize);
    }

    public int getNbTasksRunning() {
        return executor.getActiveCount();
    }

    public AsyncEventExecutor() {
        this(4, 16, 4, QUEUE_SIZE);
    }

    public AsyncEventExecutor(int poolSize, int maxPoolSize, int keepAliveTime) {
        this(poolSize, maxPoolSize, keepAliveTime, QUEUE_SIZE);
    }

    public AsyncEventExecutor(int poolSize, int maxPoolSize, int keepAliveTime, int queueSize) {
        queue = new LinkedBlockingQueue<Runnable>(queueSize);
        executor = new ThreadPoolExecutor(poolSize, maxPoolSize,
                keepAliveTime, TimeUnit.SECONDS, queue);
    }

    public void run(List<PostCommitEventListener> listeners, EventBundle event) {
        for (PostCommitEventListener listener : listeners) {
            run(listener, event);
        }
    }

    public void run(PostCommitEventListener listener, EventBundle event) {
        executor.execute(new Job(listener, event));
    }

    public static class Job implements Runnable {

        protected final ReconnectedEventBundle bundle;
        protected final PostCommitEventListener listener;

        public Job(PostCommitEventListener listener, EventBundle bundle) {
            this.listener = listener;

            if (bundle instanceof ReconnectedEventBundle) {
                this.bundle = (ReconnectedEventBundle) bundle;
            } else {
                this.bundle = new ReconnectedEventBundleImpl(bundle);
            }
        }

        public void run() {
            EventBundleTransactionHandler txh = new EventBundleTransactionHandler();
            try {
                txh.beginNewTransaction();
                listener.handleEvent(bundle);
                txh.commitOrRollbackTransaction();
                log.debug("Async listener executed, commiting tx");
            } catch (Throwable t) {
                log.error("Failed to execute async event " + bundle.getName() + " on listener " + listener, t);
                txh.rollbackTransaction();
            } finally {
                bundle.disconnect();
            }
        }
    }

}
