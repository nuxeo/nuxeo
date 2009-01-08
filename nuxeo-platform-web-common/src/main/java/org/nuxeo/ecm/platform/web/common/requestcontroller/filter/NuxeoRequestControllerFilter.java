package org.nuxeo.ecm.platform.web.common.requestcontroller.filter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestControllerManager;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestFilterConfig;
import org.nuxeo.ecm.platform.web.common.tx.TransactionsHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Filter to handle Transactions and Requests synchronization.
 * This filter is useful when accessing web resources that are not
 * protected by Seam Filter.
 * This is the case for specific Servlets, WebEngine, XML-RPC connector ...
 *
 * @author tiry
 */
public class NuxeoRequestControllerFilter implements Filter {

    protected static final String SESSION_LOCK_KEY = "NuxeoSessionLockKey";
    protected static final String SYNCED_REQUEST_FLAG = "NuxeoSessionAlreadySync";

    protected static final int LOCK_TIMOUT_S = 120;

    protected static RequestControllerManager rcm;

    private static final Log log = LogFactory.getLog(NuxeoRequestControllerFilter.class);

    public void init(FilterConfig filterConfig) throws ServletException {
        rcm = Framework.getLocalService(RequestControllerManager.class);

        if (rcm == null) {
            log.error("Unable to get RequestControlerManager service");
            throw new ServletException("RequestControlerManager can not be found");
        }
        log.debug("Staring NuxeoRequestControler filter");
    }

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        log.debug("Entering NuxeoRequestControler filter");

        RequestFilterConfig config = rcm.getConfigForRequest(httpRequest);

        String targetURI = httpRequest.getRequestURI();
        boolean useSync = config.needSynchronization();
        boolean useTx = config.needTransaction();

        if (!useSync && !useTx) {
            log.debug("Existing NuxeoRequestControler filter : nothing to be done for uri " + targetURI);
            chain.doFilter(request, response);
            return;
        }

        log.debug("Handling request on " + targetURI + " with tx=" + useTx + " and sync=" + useSync);

        boolean sessionSynched=false;
        if (useSync) {
            sessionSynched = simpleSyncOnSession(httpRequest);
        }
        boolean txStarted = false;
        try {
            if (useTx) {
                txStarted = startUserTransaction();
            }
            chain.doFilter(request, response);
        } catch (Exception e) {
            log.error("Unhandled error was cauth by the Filter", e);
            if (txStarted) {
                log.debug("Marking transaction for RollBack");
                markTransactionForRollBack();
            }
            throw new ServletException(e);
        } finally {
            if (txStarted) {
                commitOrRollBackUserTransaction();
            }
            if (sessionSynched) {
                simpleReleaseSyncOnSession(httpRequest);
            }
            log.debug("Exiting NuxeoRequestControler filter");
        }
    }

    /**
     * Starts a new {@link UserTransaction}.
     *
     * @return true if the transaction was successfully translated, false otherwise
     */
    protected boolean startUserTransaction() {
        try {
            TransactionsHelper.getUserTransaction().begin();
        } catch (Exception e) {
            log.error("Unable to start transaction", e);
            return false;
        }
        return true;
    }

    /**
     * Marks the {@link UserTransaction} for rollBack.
     */
    protected void markTransactionForRollBack() {
        try {
            TransactionsHelper.getUserTransaction().setRollbackOnly();
            log.debug("NuxeoRequestControler setting transaction to RollBackOnly");
        } catch (Exception e) {
            log.error("Unable to rollback transaction", e);
        }
    }

    /**
     * Commits or rollbacks the {@link UserTransaction} depending on the Transaction status.
     */
    protected void commitOrRollBackUserTransaction() {
        try {
            if (TransactionsHelper.isTransactionActiveOrMarkedRollback()) {
                if (TransactionsHelper.isTransactionMarkedRollback()) {
                    log.debug("can not commit transaction since it is marked RollBack only");
                    TransactionsHelper.getUserTransaction().rollback();
                } else {
                    log.debug("NuxeoRequestControler commiting transaction");
                    TransactionsHelper.getUserTransaction().commit();
                }
            }
        } catch (Exception e) {
            log.error("Unable to commit/rollback transaction", e);
        }
    }

    /**
     * Synchronizes the HttpSession.
     * <p>
     * Uses a {@link Lock} object in the HttpSession and locks it.
     * If HttpSession is not created, exits without locking anything.
     */
    protected boolean simpleSyncOnSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            log.debug("HttpSession does not exist, this request won't be synched");
            return false;
        }

        log.debug("Trying to sync on session " + session.getId() + " on Thread " + Thread.currentThread().getId());

        Lock lock = (Lock) session.getAttribute(SESSION_LOCK_KEY);
        if (lock == null) {
            lock = new ReentrantLock();
            session.setAttribute(SESSION_LOCK_KEY, lock);
        }
        if (request.getAttribute(SYNCED_REQUEST_FLAG) != null) {
            log.warn("Request has already be synced, filter is reentrant, exiting without locking");
            return false;
        }

        boolean locked = false;
        try {
            locked = lock.tryLock(LOCK_TIMOUT_S, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Unable to acuire lock for Session sync", e);
            return false;
        }

        if (locked) {
            request.setAttribute(SYNCED_REQUEST_FLAG, true);
        }
        return locked;
    }

    /**
     * Releases the {@link Lock} if present in the HttpSession.
     */
    protected void simpleReleaseSyncOnSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            log.debug("No more HttpSession : can not unlock !, HttpSession must have been invalidated");
            return;
        }
        log.debug("Trying to unlock on session " + session.getId() + " on Thread " + Thread.currentThread().getId());

        Lock lock = (Lock) session.getAttribute(SESSION_LOCK_KEY);
        if (lock == null) {
            log.error("Unable to find session lock, HttpSession may have been invalidated");
        } else {
            lock.unlock();
            log.debug("session " + session.getId() + " unlocked on Thread " + Thread.currentThread().getId());
        }
    }

    public void destroy() {
        rcm = null;
    }

}
