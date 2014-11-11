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
 *     matic
 */
package org.nuxeo.ecm.platform.management.usecases;

import java.util.Date;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

public class UsecaseContext {

    @SuppressWarnings("unused")
    private final UsecaseSchedulerService scheduler;

    protected UsecaseContext(UsecaseSchedulerService usecaseSchedulerService,
            Usecase usecase, String repositoryName) {
        this.scheduler = usecaseSchedulerService;
        this.usecase = usecase;
        this.runner = new RepositoryRunner(repositoryName);
    }

    protected boolean isEnabled = true;

    protected String shortcutName;

    protected String qualifiedName;

    protected final Usecase usecase;

    protected long runnedCount = 0L;

    protected Date lastRunnedDate;

    protected long lastDuration = 0L;

    protected long succeedCount = 0L;

    protected Date lastSucceedDate;

    protected long failedCount = 0L;

    protected Date lastFailedDate;

    protected Exception lastFailedCause;

    protected class RepositoryRunner extends UnrestrictedSessionRunner {

        public RepositoryRunner(String repositoryName) {
            super(repositoryName);
        }

        public void runWithSafeClassLoader() {
            Thread currentThread = Thread.currentThread();
            ClassLoader lastLoader = currentThread.getContextClassLoader();
            currentThread.setContextClassLoader(RepositoryRunner.class.getClassLoader());
            try {
                UsecaseContext.this.runner.runUnrestricted();
            } catch (ClientException e) {
                ;
            } finally {
                currentThread.setContextClassLoader(lastLoader);
            }
        }

        @Override
        public synchronized void run() throws ClientException {
            if (isEnabled == false) {
                return;
            }
            Date startingDate = new Date();
            try {
                usecase.runCase(session);
                succeedCount += 1;
                lastSucceedDate = startingDate;
            } catch (ClientException e) {
                failedCount += 1;
                lastFailedDate = new Date();
                lastFailedCause = e;
                throw e;
            } catch (RuntimeException e) {
                failedCount += 1;
                lastFailedDate = new Date();
                lastFailedCause = e;
                throw new ClientException(e); // avoid breaking main loop
            } finally {
                runnedCount += 1;
                lastRunnedDate = startingDate;
                lastDuration = doGetDuration(startingDate, new Date());
            }
        }
    }

    protected final RepositoryRunner runner;

    public boolean isInError() {
        if (lastFailedDate == null)
            return false;
        if (lastSucceedDate != null)
            return lastFailedDate.after(lastSucceedDate);
        return true;
    }

    protected UsecaseMBean getMBean() {
        return new UsecaseMBean() {

            public boolean isInError() {
                return UsecaseContext.this.isInError();
            }

            public long getFailedCount() {
                return failedCount;
            }

            public long getLastDuration() {
                return lastDuration;
            }

            public Exception getLastFailedCause() {
                return lastFailedCause;
            }

            public Date getLastFailedDate() {
                return lastFailedDate;
            }

            public Date getLastRunnedDate() {
                return lastRunnedDate;
            }

            public Date getLastSucceedDate() {
                return lastSucceedDate;
            }

            public long getRunnedCount() {
                return runnedCount;
            }

            public long getSucceedCount() {
                return succeedCount;
            }

            public void run() {
                Thread currentThread = Thread.currentThread();
                ClassLoader lastLoader = currentThread.getContextClassLoader();
                currentThread.setContextClassLoader(UsecaseContext.class.getClassLoader());
                try {
                    UsecaseContext.this.runner.runUnrestricted();
                } catch (ClientException e) {
                    ;
                } finally {
                    currentThread.setContextClassLoader(lastLoader);
                }
            }

            public void disable() {
                UsecaseContext.this.isEnabled = false;
            }

            public void enable() {
                UsecaseContext.this.isEnabled = true;
            }

            public boolean isEnabled() {
                return UsecaseContext.this.isEnabled;
            }
        };
    }

    protected Long doGetDuration(Date fromDate, Date toDate) {
        return toDate.getTime() - fromDate.getTime();
    }

}