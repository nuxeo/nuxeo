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
 *     anguenot
 *
 * $Id: CompassBackendSession.java 30087 2008-02-12 16:10:09Z ogrisel $
 */

package org.nuxeo.ecm.core.search.backend.compass;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.compass.core.Compass;
import org.compass.core.CompassException;
import org.compass.core.CompassSession;
import org.compass.core.CompassTransaction;
import org.compass.core.Resource;
import org.nuxeo.ecm.core.search.session.SearchServiceSessionImpl;
import org.nuxeo.ecm.core.search.transaction.Transactions;

/**
 * Compass backend session: wraps a Compass object and defines a queue holding
 * pending Compass resources that can be flushed on demand.
 * <p>
 * <code>CompassBackendSession</code> should be associate to only one thread.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class CompassBackendSession extends SearchServiceSessionImpl {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(CompassBackendSession.class);

    protected Compass compass;

    protected final Queue<Resource> resourcesQueue;

    protected CompassTransaction txn;

    protected CompassSession session;

    public CompassBackendSession() {
        resourcesQueue = new LinkedBlockingQueue<Resource>();
    }

    public CompassBackendSession(String sid) {
        super(sid);
        resourcesQueue = new LinkedBlockingQueue<Resource>();
    }

    public void begin(Compass compass) {
        this.compass = compass;
        openSession();
        beginTransaction();
    }

    public boolean hasCompass() {
        return compass != null;
    }

    public void add(Resource resource) {
        resourcesQueue.add(resource);
        if (resource != null && log.isDebugEnabled()) {
            log.debug(String.format("Resource %s queued", resource.getId()));
        }
    }

    public void save() {
        if (resourcesQueue == null) {
            log.warn("Resource queue is empty cannot save anything here");
            return;
        }

        final Queue<Resource> res = new LinkedBlockingQueue<Resource>();
        res.addAll(resourcesQueue);
        log.debug("Saving all queued resources. size="
                + Integer.toString(res.size()));
        while (!res.isEmpty()) {
            save(res.poll());
        }
        // If ok then flush no need to keep the resources since committed.
        resourcesQueue.clear();
    }

    public void save(Resource resource) {
        session.save(resource);
        log.debug("Saving one resource");
    }

    public CompassSession getCompassSession() {
        return session;
    }

    public CompassTransaction getCompassTxn() {
        return txn;
    }

    public int countWaitingResources() {
        return resourcesQueue.size();
    }

    public CompassSession openSession() {
        log.debug("Opening a new Compass Session");
        session = compass.openSession();
        return session;
    }

    public boolean isSessionOpened() {
        return session != null;
    }

    public CompassTransaction beginTransaction() {
        log.debug("Starting a new Compass transaction");
        txn = session.beginTransaction();
        return txn;
    }

    public boolean isTransactionStarted() {
        return txn != null;
    }

    public void clean() {
        session = null;
        txn = null;
        log.debug("Clean session");
    }

    // XXX move this search service side.
    protected void beginUTransaction() {
        try {
            log.debug("Beginning transaction prior to flushing sessions");
            Transactions.getUserTransaction().begin();
        } catch (Exception e) {
            throw new IllegalStateException("Could not start transaction", e);
        }
    }

    // XXX move this search service side.
    protected void commitOrRollbackUTransaction() {
        try {
            if (Transactions.isTransactionActive()) {
                log.debug("committing transaction after flushing sessions");
                Transactions.getUserTransaction().commit();
            } else if (Transactions.isTransactionMarkedRollback()) {
                log.debug("rolling back transaction after flushing sessions");
                Transactions.getUserTransaction().rollback();
            }
        } catch (Exception e) {
            // TODO: what should we *really* do here??
            throw new IllegalStateException("Could not commit transaction", e);
        }
    }

    public void saveAndCommit(boolean userTxn) throws CompassException {
        try {
            save();
            txn.commit();
            session.close();
            // clean();
        } catch (CompassException ce) {
            log.error(ce);
            txn.rollback();
        } finally {
            if (userTxn) {
                commitOrRollbackUTransaction();
            }

        }
    }

    public void rollback() {
        if (txn != null) {
            txn.rollback();
        }
        if (session != null) {
            session.close();
        }
        clean();
    }

}
