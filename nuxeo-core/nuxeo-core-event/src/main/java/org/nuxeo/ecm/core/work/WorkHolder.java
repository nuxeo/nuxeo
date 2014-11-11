/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.work;

import java.util.concurrent.ThreadPoolExecutor;

import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkSchedulePath;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * A {@link WorkHolder} adapts a {@link Work} to {@link Runnable} for queuing
 * and execution by a {@link ThreadPoolExecutor}.
 * <p>
 * It also deals with transaction management, and prevents running work
 * instances that are suspending.
 * <p>
 * Calls {@link Work#work} and {@link Work#cleanUp}.
 *
 * @see Work
 * @see Work#work
 * @see Work#cleanUp
 * @see AbstractWork
 * @since 5.8
 */
public class WorkHolder implements Runnable {

    private final Work work;

    public WorkHolder(Work work) {
        this.work = work;
    }

    public static Work getWork(Runnable r) {
        return ((WorkHolder) r).work;
    }

    @Override
    public void run() {
        if (work.isSuspending()) {
            // don't run anything if we're being started while a suspend
            // has been requested
            work.suspended();
            return;
        }
        TransactionHelper.startTransaction();
        WorkSchedulePath.handleEnter(work);
        boolean ok = false;
        Exception exc = null;
        try {
            work.setStartTime();
            work.work();
            ok = true;
        } catch (Exception e) { // InterruptedException managed below
            exc = e;
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        } finally {
            WorkSchedulePath.handleReturn();
            try {
                work.cleanUp(ok, exc);
            } finally {
                try {
                    if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
                        if (!ok) {
                            TransactionHelper.setTransactionRollbackOnly();
                        }
                        TransactionHelper.commitOrRollbackTransaction();
                    }
                } finally {
                    if (exc instanceof InterruptedException) {
                        // restore interrupted status for the thread pool
                        // worker
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

}
