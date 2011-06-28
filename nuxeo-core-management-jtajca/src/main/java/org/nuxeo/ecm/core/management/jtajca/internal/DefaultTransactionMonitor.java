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

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
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

    protected TransactionManagerImpl tm;

    protected static MBeanServer mbs;

    private DefaultTransactionMonitor(TransactionManagerImpl manager) {
        this.tm = manager;
    }

    protected static final Log log = LogFactory.getLog(DefaultTransactionMonitor.class);

    protected static DefaultTransactionMonitor monitor;

    public static void install() {
        TransactionManagerImpl tm = lookup();
        monitor = new DefaultTransactionMonitor(tm);
        tm.addTransactionAssociationListener(monitor);
        bindManagementInterface();
    }

    public static void uninstall() throws MBeanRegistrationException,
            InstanceNotFoundException {
        if (monitor == null) {
            return;
        }
        unbindManagementInterface();
        monitor.tm.removeTransactionAssociationListener(monitor);
        monitor = null;
    }

    protected static void bindManagementInterface() {
        try {
            mbs = ManagementFactory.getPlatformMBeanServer();
            mbs.registerMBean(monitor, new ObjectName(TransactionMonitor.NAME));
        } catch (Exception cause) {
            throw new Error("Cannot register tx monitor", cause);
        }
    }

    protected static void unbindManagementInterface() {
        try {
            mbs.unregisterMBean(new ObjectName(TransactionMonitor.NAME));
        } catch (Exception e) {
            throw new Error("Cannot unregister tx monitor");
        } finally {
            mbs = null;
        }
    }

    public static TransactionManagerImpl lookup() {
        TransactionManager tm = NuxeoContainer.getTransactionManager();
        if (tm == null) { // try setup trough NuxeoTransactionManagerFactory
            try {
                InitialContext ic = new InitialContext();
                tm = (TransactionManager) ic.lookup("java:comp/env/TransactionManager");
            } catch (NamingException cause) {
                throw new Error("Cannot lookup tx manager", cause);
            }
        }
        if (!(tm instanceof NuxeoContainer.TransactionManagerWrapper)) {
            throw new Error("Nuxeo container not installed");
        }
        try {
            Field f = NuxeoContainer.TransactionManagerWrapper.class.getDeclaredField("tm");
            f.setAccessible(true);
            return (TransactionManagerImpl) f.get(tm);
        } catch (Exception cause) {
            throw new Error("Cannot access to geronimo tx manager", cause);
        }
    }

    protected TransactionStatistics lastCommittedStatistics;

    protected TransactionStatistics lastRollbackedStatistics;

    protected final Map<Object, DefaultTransactionStatistics> activeStatistics = new HashMap<Object, DefaultTransactionStatistics>();

    public static String id(Transaction tx) {
        return Integer.toHexString(System.identityHashCode(tx));
    }

    @Override
    public void threadAssociated(Transaction tx) {
        long now = System.currentTimeMillis();
        Object key = tm.getTransactionKey();
        MDC.put("TX", key.toString());
        log.trace("associated tx with thread");
        Stopwatch sw = SimonManager.getStopwatch("tx");
        final Thread thread = Thread.currentThread();
        DefaultTransactionStatistics info = new DefaultTransactionStatistics(
                key);
        info.split = sw.start();
        info.threadName = thread.getName();
        info.status = TransactionStatistics.Status.ACTIVE;
        info.startTimestamp = now;
        info.startCapturedContext = new Throwable("** start invoke context **");
        synchronized (this) {
            activeStatistics.put(key, info);
        }
        tm.registerInterposedSynchronization(monitor); // register end status
    }

    @Override
    public void threadUnassociated(Transaction tx) {
        long now = System.currentTimeMillis();
        Object key = ((TransactionImpl) tx).getTransactionKey();
        MDC.remove("TX");
        DefaultTransactionStatistics stats;
        synchronized (DefaultTransactionMonitor.class) {
            stats = (DefaultTransactionStatistics) activeStatistics.remove(key);
        }
        if (stats == null) {
            log.error(key + " not found in active statitics map");
            return;
        }
        stats.split.stop();
        stats.split = null;
        stats.endTimestamp = now;
        log.trace(stats);
        if (TransactionStatistics.Status.COMMITTED.equals(stats.status)) {
            lastCommittedStatistics = stats;
        } else if (TransactionStatistics.Status.ROLLEDBACK.equals(stats.status)) {
            lastRollbackedStatistics = stats;
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
            stats = (DefaultTransactionStatistics) activeStatistics.get(key);
        }
        if (stats == null) {
            log.error(key + " not found in active statitics map");
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
    public void afterCompletion(int status) {
        DefaultTransactionStatistics stats = thisStatistics();
        if (stats == null) {
            return;
        }
        switch (status) {
        case Status.STATUS_COMMITTED:
            stats.status = TransactionStatistics.Status.COMMITTED;
            lastCommittedStatistics = stats;
            break;
        case Status.STATUS_ROLLEDBACK:
            stats.status = TransactionStatistics.Status.ROLLEDBACK;
            ;
            lastRollbackedStatistics = stats;
            stats.endCapturedContext = new Throwable(); 
            break;
        }
    }

}
