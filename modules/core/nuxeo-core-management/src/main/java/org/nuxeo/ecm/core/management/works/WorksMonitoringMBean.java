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

/**
 * Manage works from the JMX console
 *
 * @since 5.8
 * @author Stephane Lacoin at Nuxeo (aka matic)
 */
public interface WorksMonitoringMBean {

    /**
     * Is at least one queue is processing works ?
     *
     * @since 8.3
     */
    boolean isProcessing();

    /**
     * Toggles processing on all queues
     *
     * @since 8.3
     */
    boolean toggleProcessing();

    /**
     * Enable/disable work schedule stack capture
     */
    boolean toggleScheduleStackCapture();

    /**
     * is work schedule stack capture enabled ?
     */
    boolean isScheduleStackCapture();

}
