package org.nuxeo.ecm.core.event.tx;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;

/**
 *
 * Runs synchronous Listeners in a separated thread in order to enable TX management
 *
 * @author tiry
 *
 */
public class PostCommitSynchronousRunner {

    protected EventBundleTransactionHandler txh = new EventBundleTransactionHandler();

    private static final Log log = LogFactory.getLog(PostCommitSynchronousRunner.class);

    protected List<PostCommitEventListener> listeners = null;
    protected EventBundle event = null;
    protected long timeout=0;

    public PostCommitSynchronousRunner(List<PostCommitEventListener> listeners, EventBundle event, long timeout) {
        this.listeners=listeners;
        this.event=event;
        this.timeout=timeout;
    }

    public PostCommitSynchronousRunner(List<PostCommitEventListener> listeners, EventBundle event) {
        this(listeners, event, 0);
    }

    public void run() {
        if (timeout==0) {
            runSync();
        } else {
            runSyncWithTimeOut(timeout);
        }
    }

    protected void runSync() {
        Thread runner = new Thread(new MonoThreadExecutor());
        runner.run();
    }

    protected void runSyncWithTimeOut(long timeout) {
        Thread runner = new Thread(new MonoThreadExecutor());
        runner.start();
        try {
            this.wait(timeout);
        } catch (InterruptedException e) {
            log.error("Exit before the end of processing", e);
        }
    }

    protected class MonoThreadExecutor implements Runnable {

        protected Object waiter;

        public MonoThreadExecutor() {
        }

        public MonoThreadExecutor(Object waiter) {
            this.waiter = waiter;
        }

        public void run() {
            long t0 = System.currentTimeMillis();
            log.debug("Start post commit sync execution");
            for (PostCommitEventListener listener : listeners) {
                try {
                    txh.beginNewTransaction();
                    listener.handleEvent(event);
                    txh.commitOrRollbackTransaction();
                }
                catch (Throwable t) {
                    txh.rollbackTransaction();
                }
            }
            log.debug("End of post commit sync execution : " + (System.currentTimeMillis()-t0) + "ms");

            if (waiter!=null) {
                waiter.notify();
            }
        }

    }


}
