/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.ecm.core.management.probes;

import java.util.Date;

import org.nuxeo.ecm.core.management.api.ProbeInfo;
import org.nuxeo.ecm.core.management.api.ProbeMBean;
import org.nuxeo.ecm.core.management.api.ProbeStatus;

public class ProbeInfoImpl implements ProbeMBean, ProbeInfo {

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

}
