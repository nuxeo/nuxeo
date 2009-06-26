package org.nuxeo.ecm.core.event.tx;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;

/**
 *
 * @author Thierry Delprat
 *
 */
public class BulkExecutor extends PostCommitSynchronousRunner {

    private static final Log log = LogFactory.getLog(BulkExecutor.class);

    public BulkExecutor(List<EventListenerDescriptor> listeners,
            EventBundle event) {
        super(listeners, event, 0);
    }

    @Override
    protected Runnable getExecutor() {
        return new MonoThreadBulkExecutor();
    }

    protected class MonoThreadBulkExecutor implements Runnable {

        public void run() {
            EventBundleTransactionHandler txh = new EventBundleTransactionHandler();
            long t0 = System.currentTimeMillis();
            log.debug("Start post commit sync execution in Thread "
                    + Thread.currentThread().getId());
            txh.beginNewTransaction();
            try {
                for (EventListenerDescriptor listener : listeners) {
                    listener.asPostCommitListener().handleEvent(event);
                }
                txh.commitOrRollbackTransaction();
            } catch (Throwable t) {
                txh.rollbackTransaction();
            }
            log.debug("End of all post commit sync executions : "
                    + (System.currentTimeMillis() - t0) + "ms");
        }
    }
}
