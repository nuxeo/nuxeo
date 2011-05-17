/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.runtime.management.counters;

/**
 * Service interface to manage Counters.
 *
 * This services hides the Counters implementation so that Counters's updated
 * don't have to be dependent on Simon
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public interface CounterManager {

    /**
     * Increase a counter
     *
     * @param counterName
     */
    void increaseCounter(String counterName);


    void increaseCounter(String counterName, long value);

    /**
     * Set the value of a counter
     *
     * @param counterName
     * @param value
     */
    void setCounterValue(String counterName, long value);

    /**
     * Decrease a counter
     *
     * @param counterName
     */
    void decreaseCounter(String counterName);

    void decreaseCounter(String counterName, long value);

    /**
     * Get recorder values of the counter over time
     * @param counterName
     * @return
     */
    CounterHistoryStack getCounterHistory(String counterName);


    /**
     * Enables all counters
     *
     */
    void enableCounters();

    /**
     * Desable all couters
     *
     */
    void disableCounters();

}
