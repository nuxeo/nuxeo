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
package org.nuxeo.ecm.core.management.statuses;

import java.util.Date;

public class ProbeInfo implements ProbeMBean {

    protected ProbeInfo(ProbeDescriptor descriptor, Probe probe) {
        this.descriptor = descriptor;
        this.probe = probe;
    }

    protected ProbeDescriptor descriptor;

    protected boolean isEnabled = true;

    protected String shortcutName;

    protected String qualifiedName;

    protected final Probe probe;

    protected ProbeStatus lastStatus = new ProbeStatus("not yet runned", false);

    protected long runnedCount = 0L;

    protected Date lastRunnedDate;

    protected long lastDuration = 0L;

    protected long successCount = 0L;

    protected Date lastSucceedDate = new Date(0);

    protected ProbeStatus lastSuccesStatus = ProbeStatus.newSuccess("not yet succeed");

    protected long failureCount = 0L;

    protected Date lastFailureDate = new Date(0);

    protected ProbeStatus lastFailureStatus = ProbeStatus.newFailure("not yet failed");

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
        ProbeInfo.this.isEnabled = false;
    }

    public void enable() {
        ProbeInfo.this.isEnabled = true;
    }

    public boolean isEnabled() {
        return ProbeInfo.this.isEnabled;
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
        return lastStatus;
    }

    public String getShortcutName() {
        return shortcutName;

    }

    protected static Long doGetDuration(Date fromDate, Date toDate) {
        return toDate.getTime() - fromDate.getTime();
    }

    public synchronized void run()  {
        if (!isEnabled) {
            return;
        }
        Thread currentThread = Thread.currentThread();
        ClassLoader lastLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(ProbeInfo.class.getClassLoader());
        lastRunnedDate = new Date();
        runnedCount += 1;
        try {
            lastStatus = probe.run();
            if (lastStatus.isSuccess()) {
                lastSucceedDate = lastRunnedDate;
                lastSuccesStatus = lastStatus;
                successCount += 1;
            } else {
                lastFailureStatus = lastStatus;
                failureCount += 1;
                lastFailureDate = lastRunnedDate;
            }
        } catch (Throwable e) {
            failureCount += 1;
            lastFailureDate = new Date();
            lastFailureStatus = ProbeStatus.newError(e);
        } finally {
            lastDuration = doGetDuration(lastRunnedDate, new Date());
            currentThread.setContextClassLoader(lastLoader);
        }
    }

}
