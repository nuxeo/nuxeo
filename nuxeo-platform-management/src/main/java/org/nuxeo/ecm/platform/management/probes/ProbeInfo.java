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
package org.nuxeo.ecm.platform.management.probes;

import java.util.Date;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

public class ProbeInfo implements ProbeMBean {

    @SuppressWarnings("unused")
    private final ProbeComponent scheduler;

    protected ProbeInfo(ProbeComponent usecaseSchedulerService, Probe usecase,
            String repositoryName) {
        scheduler = usecaseSchedulerService;
        this.usecase = usecase;
        runner = new RepositoryRunner(repositoryName);
    }

    protected boolean isEnabled = true;

    protected String shortcutName;

    protected String qualifiedName;

    protected final Probe usecase;

    protected long runnedCount = 0L;

    protected Date lastRunnedDate;

    protected long lastDuration = 0L;

    protected long succeedCount = 0L;

    protected Date lastSucceedDate;

    protected long failedCount = 0L;

    protected Date lastFailedDate;

    protected Exception lastFailedCause;

    protected ProbeStatus probeStatus;

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

    public void disable() {
        ProbeInfo.this.isEnabled = false;
    }

    public void enable() {
        ProbeInfo.this.isEnabled = true;
    }

    public boolean isEnabled() {
        return ProbeInfo.this.isEnabled;
    }

    public boolean isInError() {
        if (lastFailedDate == null) {
            return false;
        }
        if (lastSucceedDate != null) {
            return lastFailedDate.after(lastSucceedDate);
        }
        return true;
    }

    public ProbeStatus getProbeStatus() {
        return probeStatus;
    }
    
    public String getShortcutName(){
        return shortcutName;
        
    }

    public void setProbeStatus(ProbeStatus probeStatus) {
        this.probeStatus = probeStatus;
    }

    protected static Long doGetDuration(Date fromDate, Date toDate) {
        return toDate.getTime() - fromDate.getTime();
    }

    protected class RepositoryRunner extends UnrestrictedSessionRunner {

        protected RepositoryRunner(String repositoryName) {
            super(repositoryName);
        }

        public void runWithSafeClassLoader() throws ClientException {
            Thread currentThread = Thread.currentThread();
            ClassLoader lastLoader = currentThread.getContextClassLoader();
            currentThread.setContextClassLoader(RepositoryRunner.class.getClassLoader());
            try {
                ProbeInfo.this.runner.runUnrestricted();
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
                usecase.runProbe(session);
                setProbeStatus(usecase.getProbeStatus());
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

    public void run() {
        Thread currentThread = Thread.currentThread();
        ClassLoader lastLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(ProbeInfo.class.getClassLoader());
        try {
            runner.runUnrestricted();
        } catch (ClientException e) {
        } finally {
            currentThread.setContextClassLoader(lastLoader);
        }
    }

}
