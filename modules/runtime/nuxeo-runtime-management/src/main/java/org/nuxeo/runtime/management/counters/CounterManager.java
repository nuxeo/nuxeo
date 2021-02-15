/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.management.counters;

/**
 * Service interface to manage Counters. This services hides the Counters implementation so that Counters's updated
 * don't have to be dependent on Simon
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @deprecated since 11.4: use dropwizard metrics counter instead
 */
@Deprecated(since = "11.4")
public interface CounterManager {

    /**
     * Increase a counter
     */
    void increaseCounter(String counterName);

    void increaseCounter(String counterName, long value);

    /**
     * Set the value of a counter
     */
    void setCounterValue(String counterName, long value);

    /**
     * Decrease a counter
     */
    void decreaseCounter(String counterName);

    void decreaseCounter(String counterName, long value);

    /**
     * Get recorder values of the counter over time
     */
    CounterHistoryStack getCounterHistory(String counterName);

    /**
     * Enables all counters
     */
    void enableCounters();

    /**
     * Desable all couters
     */
    void disableCounters();

}
