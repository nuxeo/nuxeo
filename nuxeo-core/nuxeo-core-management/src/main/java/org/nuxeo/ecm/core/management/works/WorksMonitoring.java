/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.core.management.works;


import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkSchedulePath;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.8
 * @author Stephane Lacoin at Nuxeo (aka matic)
 */
public class WorksMonitoring implements WorksMonitoringMBean {

    protected WorkManager manager() {
        return Framework.getService(WorkManager.class);
    }

    @Override
    public boolean toggleScheduleStackCapture() {
        return WorkSchedulePath.toggleCaptureStack();
    }

    @Override
    public boolean isScheduleStackCapture() {
        return WorkSchedulePath.isCaptureStackEnabled();
    }

    @Override
    public boolean isProcessing() {
        return manager().isProcessingEnabled();
    }

    @Override
    public boolean toggleProcessing() {
        final boolean processing = !manager().isProcessingEnabled();
        manager().enableProcessing(processing);
        return processing;
    }

}
