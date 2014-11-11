/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.core.management.works;

import javax.resource.spi.work.WorkManager;

import org.nuxeo.ecm.core.work.api.WorkSchedulePath;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.8
 * @author Stephane Lacoin at Nuxeo (aka matic)
 *
 */
public class WorksMonitoring implements WorksMonitoringMBean {

    protected WorkManager manager() {
        return Framework.getLocalService(WorkManager.class);
    }

    @Override
    public void toggleSchedulePathCapture() {
        WorkSchedulePath.toggleCapturePath();
    }

    @Override
    public boolean isSchedulePathCaptureEnabled() {
        return WorkSchedulePath.capturePath;
    }

    @Override
    public void toggleScheduleStackCapture() {
        WorkSchedulePath.toggleCaptureStack();
    }

    @Override
    public boolean isScheduleStackCapture() {
        return WorkSchedulePath.capturePath;
    }

}
