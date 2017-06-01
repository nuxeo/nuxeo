/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.management.works;

import javax.management.MXBean;

@MXBean
public interface WorksQueueMonitoringMBean {

    /**
     * Gets scheduled/running/completed/cancelled counters at once
     *
     * @since 8.3
     */
    long[] getMetrics();

    /**
     * Is at least one queue is processing works ?
     *
     * @since 8.3
     */
    boolean isProcessing();

    /**
     * Toogles processing for all queues
     *
     * @since 8.3
     */
    boolean toggleProcessing();

}

