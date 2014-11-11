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
package org.nuxeo.ecm.platform.management.statuses;

import java.util.Date;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;

public class ProbeInfo implements ProbeMBean {

    @SuppressWarnings("unused")
    private final StatusesManagementComponent scheduler;

    protected ProbeInfo(StatusesManagementComponent usecaseSchedulerService, Probe probe) {
        scheduler = usecaseSchedulerService;
        this.probe = probe;
    }

    protected boolean isEnabled = true;

    protected String shortcutName;

    protected String qualifiedName;

    protected final Probe probe;

    protected long runnedCount = 0L;

    protected Date lastRunnedDate;

    protected long lastDuration = 0L;

    protected long successCount = 0L;

    protected Date lastSucceedDate;

    protected ProbeStatus lastSuccessStatus;

    protected long failureCount = 0L;

    protected Date lastFailureDate;

    protected ProbeStatus lastFailureStatus;


    public long getFailedCount() {
        return failureCount;
    }

    public long getLastDuration() {
        return lastDuration;
    }

    public ProbeStatus getLastFailureStatus() {
        return lastFailureStatus;
    }

    public Date getLastFailedDate() {
        return lastFailureDate;
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
        return successCount;
    }

    public void disable() {
        isEnabled = false;
    }

    public void enable() {
        isEnabled = true;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public boolean isInError() {
        if (lastFailureDate == null) {
            return false;
        }
        if (lastSucceedDate != null) {
            return lastFailureDate.after(lastSucceedDate);
        }
        return true;
    }

    public ProbeStatus getStatus() {
        if (lastFailureStatus == null && lastSuccessStatus == null) {
            return ProbeStatus.newFailure("not yet runned");
        }
        if (isInError()) {
            return lastFailureStatus;
        }
        return lastSuccessStatus;
    }

    public String getShortcutName(){
        return shortcutName;

    }

    public void setProbeStatus(ProbeStatus probeStatus) {
        lastSuccessStatus = probeStatus;
    }

    protected static Long doGetDuration(Date fromDate, Date toDate) {
        return toDate.getTime() - fromDate.getTime();
    }

    protected class RepositoryRunner extends UnrestrictedSessionRunner {

        protected RepositoryRunner() {
            super(Framework.getLocalService(RepositoryManager.class).getDefaultRepository().getName());
        }

        public void runWithSafeClassLoader() throws ClientException {
            Thread currentThread = Thread.currentThread();
            ClassLoader lastLoader = currentThread.getContextClassLoader();
            currentThread.setContextClassLoader(RepositoryRunner.class.getClassLoader());
            try {
            runUnrestricted();
            } finally {
                currentThread.setContextClassLoader(lastLoader);
            }
        }

        @Override
        public synchronized void run() throws ClientException {
            if (!isEnabled) {
                return;
            }
            Date startingDate = new Date();
            try {
                ProbeStatus status = probe.runProbe(session);
                if (status.isSuccess()) {
                    lastSucceedDate = startingDate;
                    lastSuccessStatus = status;
                    successCount += 1;
                } else {
                    lastFailureStatus = status;
                    failureCount += 1;
                    lastFailureDate = startingDate;
                }
            } catch (Throwable e) {
                failureCount += 1;
                lastFailureDate = new Date();
                lastFailureStatus = ProbeStatus.newError(e);
           } finally {
                runnedCount += 1;
                lastRunnedDate = startingDate;
                lastDuration = doGetDuration(startingDate, new Date());
            }
        }
    }


    public void run() {
        Thread currentThread = Thread.currentThread();
        ClassLoader lastLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(ProbeInfo.class.getClassLoader());
        try {
            RepositoryRunner runner = new RepositoryRunner();
            runner.runWithSafeClassLoader();
        } catch (ClientException e) {
        } finally {
            currentThread.setContextClassLoader(lastLoader);
        }
    }

}
