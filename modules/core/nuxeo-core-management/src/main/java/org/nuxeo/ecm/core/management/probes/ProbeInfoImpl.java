/*
 * (C) Copyright 2006-2021 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.ecm.core.management.probes;

import java.io.Serializable;
import java.util.Date;

import org.nuxeo.ecm.core.management.api.ProbeInfo;
import org.nuxeo.ecm.core.management.api.ProbeMBean;
import org.nuxeo.runtime.management.api.ProbeStatus;

/**
 * @since 5.4
 */
public class ProbeInfoImpl implements ProbeMBean, ProbeInfo, Serializable {

    private static final long serialVersionUID = 1L;

    protected final ProbeDescriptor descriptor;

    protected boolean isEnabled = true;

    protected String shortcutName;

    protected String qualifiedName;

    protected ProbeStatus lastStatus = ProbeStatus.newBlankProbStatus();

    protected long runnedCount = 0L;

    protected Date lastRunnedDate;

    protected long lastDuration = 0L;

    protected long successCount = 0L;

    protected Date lastSucceedDate = new Date(0);

    protected ProbeStatus lastSuccessStatus = ProbeStatus.newBlankProbStatus();

    protected long failureCount = 0L;

    protected Date lastFailureDate = new Date(0);

    protected ProbeStatus lastFailureStatus = ProbeStatus.newBlankProbStatus();

    protected ProbeInfoImpl(ProbeDescriptor descriptor) {
        this.descriptor = descriptor;
        shortcutName = descriptor.getShortcut();
        qualifiedName = descriptor.getQualifiedName();
    }

    @Override
    public long getFailedCount() {
        return failureCount;
    }

    @Override
    public long getLastDuration() {
        return lastDuration;
    }

    @Override
    public ProbeStatus getLastFailureStatus() {
        return lastFailureStatus;
    }

    @Override
    public Date getLastFailedDate() {
        return lastFailureDate;
    }

    @Override
    public Date getLastRunnedDate() {
        return lastRunnedDate;
    }

    @Override
    public Date getLastSucceedDate() {
        return lastSucceedDate;
    }

    @Override
    public long getRunnedCount() {
        return runnedCount;
    }

    @Override
    public long getSucceedCount() {
        return successCount;
    }

    @Override
    public void disable() {
        isEnabled = false;
    }

    @Override
    public void enable() {
        isEnabled = true;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public boolean isInError() {
        if (lastFailureDate == null) {
            return false;
        }
        if (lastSucceedDate != null) {
            return lastFailureDate.after(lastSucceedDate);
        }
        return true;
    }

    @Override
    public ProbeStatus getStatus() {
        return lastStatus;
    }

    @Override
    public String getShortcutName() {
        return shortcutName;
    }

    @Override
    public ProbeDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public String getQualifiedName() {
        return qualifiedName;
    }

    public void setQualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }

    public void setShortcutName(String shortcutName) {
        this.shortcutName = shortcutName;
    }

    @Override
    public String toString() {
        return String.format("ProbeInfo{name=%s, status=%s, runCount=%s, runDate=%s, runDuration=%sms}", shortcutName,
                lastStatus, runnedCount, lastRunnedDate, lastDuration);
    }
}
