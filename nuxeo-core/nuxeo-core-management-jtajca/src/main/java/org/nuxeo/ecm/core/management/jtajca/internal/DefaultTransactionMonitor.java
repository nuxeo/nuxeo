/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.core.management.jtajca.internal;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.ObjectInstance;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.transaction.manager.TransactionImpl;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.apache.geronimo.transaction.manager.TransactionManagerMonitor;
import org.apache.geronimo.transaction.manager.XidImpl;
import org.apache.log4j.MDC;
import org.javasimon.SimonManager;
import org.javasimon.Stopwatch;
import org.nuxeo.ecm.core.management.jtajca.TransactionMonitor;
import org.nuxeo.ecm.core.management.jtajca.TransactionStatistics;
import org.nuxeo.runtime.jtajca.NuxeoContainer;

/**
 * @author matic
 *
 */
public class DefaultTransactionMonitor implements TransactionManagerMonitor,
        TransactionMonitor, Synchronization {

    protected static final Log log = LogFactory.getLog(DefaultTransactionMonitor.class);

    protected TransactionManagerImpl tm;

    public void install() {
        tm = lookup();
        tm.addTransactionAssociationListener(this);
        bindManagementInterface();
    }

    public void uninstall() throws MBeanRegistrationException,
            InstanceNotFoundException {
        unbindManagementInterface();
        tm.removeTransactionAssociationListener(this);
    }

    protected ObjectInstance self;

    protected void bindManagementInterface() {
        self = DefaultMonitorComponent.bind(TransactionMonitor.class, this);
    }

    protected void unbindManagementInterface() {
        DefaultMonitorComponent.unbind(self);
        self = null;
    }


    public TransactionManagerImpl lookup() {
        TransactionManager tm = NuxeoContainer.getTransactionManager();
        if (tm == null) { // try setup trough NuxeoTransactionManagerFactory
            try {
                InitialContext ic = new InitialContext();
                tm = (TransactionManager) ic.lookup("java:comp/env/TransactionManager");
            } catch (NamingException cause) {
                throw new RuntimeException("Cannot lookup tx manager", cause);
            }
        }
        if (!(tm instanceof NuxeoContainer.TransactionManagerWrapper)) {
            throw new RuntimeException("Nuxeo container not installed");
        }
        try {
            Field f = NuxeoContainer.TransactionManagerWrapper.class.getDeclaredField("tm");
            f.setAccessible(true);
            return (TransactionManagerImpl) f.get(tm);
        } catch (Exception cause) {
            throw new RuntimeException("Cannot access to geronimo tx manager",
                    cause);
        }
    }

    protected TransactionStatistics lastCommittedStatistics;

    protected TransactionStatistics lastRollbackedStatistics;

    protected final Map<Object, DefaultTransactionStatistics> activeStatistics = new HashMap<Object, DefaultTransactionStatistics>();

    public static String id(Object key) {
        if (key instanceof XidImpl) {
            byte[] globalId = ((XidImpl)key).getGlobalTransactionId();
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < globalId.length; i++) {
                buffer.append(Integer.toHexString(globalId[i]));
            }
            return buffer.toString().replaceAll("0*$", "");
        }
        return key.toString();
    }

    public static String id(Transaction tx) {
        return Integer.toHexString(tx.hashCode());
    }

    @Override
    public void threadAssociated(Transaction tx) {
        long now = System.currentTimeMillis();
        Object key = tm.getTransactionKey();
        MDC.put("tx", id(key));
        Stopwatch sw = SimonManager.getStopwatch("tx");
        final Thread thread = Thread.currentThread();
        DefaultTransactionStatistics info = new DefaultTransactionStatistics(
                key);
        info.split = sw.start();
        info.threadName = thread.getName();
        info.status = TransactionStatistics.Status.fromTx(tx);

        info.startTimestamp = now;
        info.startCapturedContext = new Throwable("** start invoke context **");
        synchronized (this) {
            activeStatistics.put(key, info);
        }
        tm.registerInterposedSynchronization(this); // register end status
        if (log.isTraceEnabled()) {
            log.trace(info.toString());
        }
    }

    @Override
    public void threadUnassociated(Transaction tx) {
        try {
            Object key = ((TransactionImpl) tx).getTransactionKey();
            DefaultTransactionStatistics stats;
            synchronized (DefaultTransactionMonitor.class) {
                stats = activeStatistics.remove(key);
            }
            if (stats == null) {
                log.debug(key + " not found in active statistics map");
                return;
            }
            stats.split.stop();
            stats.split = null;
            if (log.isTraceEnabled()) {
                log.trace(stats);
            }
            if (TransactionStatistics.Status.COMMITTED.equals(stats.status)) {
                lastCommittedStatistics = stats;
            } else if (TransactionStatistics.Status.ROLLEDBACK.equals(stats.status)) {
                lastRollbackedStatistics = stats;
            }
        } finally {
            MDC.remove("tx");
        }
    }

    @Override
    public List<TransactionStatistics> getActiveStatistics() {
        List<TransactionStatistics> l = new ArrayList<TransactionStatistics>(
                activeStatistics.values());
        Collections.sort(l, new Comparator<TransactionStatistics>() {
            @Override
            public int compare(TransactionStatistics o1,
                    TransactionStatistics o2) {
                return o1.getStartDate().compareTo(o2.getEndDate());
            }
        });
        return l;
    }


    @Override
    public long getActiveCount() {
        return tm.getActiveCount();
    }

    @Override
    public long getTotalCommits() {
        return tm.getTotalCommits();
    }

    @Override
    public long getTotalRollbacks() {
        return tm.getTotalRollbacks();
    }

    @Override
    public TransactionStatistics getLastCommittedStatistics() {
        return lastCommittedStatistics;
    }

    @Override
    public TransactionStatistics getLastRollbackedStatistics() {
        return lastRollbackedStatistics;
    }

    protected DefaultTransactionStatistics thisStatistics() {
        Object key = tm.getTransactionKey();
        DefaultTransactionStatistics stats;
        synchronized (this) {
            stats = activeStatistics.get(key);
        }
        if (stats == null) {
            log.debug(key + " not found in active statistics map");
        }
        return stats;
    }

    @Override
    public void beforeCompletion() {
        DefaultTransactionStatistics stats = thisStatistics();
        if (stats == null) {
            return;
        }
        stats.endCapturedContext = new Throwable("** end invoke context **");
    }

    @Override
    public void afterCompletion(int code) {
        DefaultTransactionStatistics stats = thisStatistics();
        if (stats == null) {
            return;
        }
        stats.endTimestamp = System.currentTimeMillis();
        stats.status = TransactionStatistics.Status.fromCode(code);
        switch (code) {
        case Status.STATUS_COMMITTED:
            lastCommittedStatistics = stats;
            break;
        case Status.STATUS_ROLLEDBACK:
            lastRollbackedStatistics = stats;
            stats.endCapturedContext = new Throwable("** rollback context **");
            break;
        }
    }

}
