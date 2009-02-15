/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.event.tx;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;

/**
 *
 * Runs synchronous Listeners in a separated thread in order to enable TX
 * management
 *
 * @author tiry
 *
 */
public class PostCommitSynchronousRunner {

    public static final int DEFAULT_TIME_OUT_MS = 300;

    private static final Log log = LogFactory.getLog(PostCommitSynchronousRunner.class);

    protected final List<PostCommitEventListener> listeners;
    protected final EventBundle event;
    protected long timeout = 0;

    public PostCommitSynchronousRunner(List<PostCommitEventListener> listeners,
            EventBundle event, long timeout) {
        this.listeners = listeners;
        this.event = event;
        this.timeout = timeout;
    }

    public PostCommitSynchronousRunner(List<PostCommitEventListener> listeners,
            EventBundle event) {
        this(listeners, event, DEFAULT_TIME_OUT_MS);
    }

    public void run() {
        runSync();
    }

    protected void runSync() {
        log.debug("Starting sync executor from Thread "
                + Thread.currentThread().getId());
        Thread runner = new Thread(new MonoThreadExecutor());
        runner.start();
        try {
            runner.join(timeout);
            if (runner.isAlive()) {
                log.warn("One of the PostCommitListener is too slow, check your listeners ...");
                log.warn("Exit before the end of processing");
            }
        } catch (InterruptedException e) {
            log.error("Exit before the end of processing", e);
        }
        log.debug("Terminated sync executor from Thread "
                + Thread.currentThread().getId());
    }

    protected class MonoThreadExecutor implements Runnable {

        public void run() {
            EventBundleTransactionHandler txh = new EventBundleTransactionHandler();
            long t0 = System.currentTimeMillis();
            log.debug("Start post commit sync execution in Thread "
                    + Thread.currentThread().getId());
            for (PostCommitEventListener listener : listeners) {
                try {
                    txh.beginNewTransaction();
                    listener.handleEvent(event);
                    txh.commitOrRollbackTransaction();
                } catch (Throwable t) {
                    txh.rollbackTransaction();
                }
            }
            log.debug("End of post commit sync execution : "
                    + (System.currentTimeMillis() - t0) + "ms");
        }

    }

}
