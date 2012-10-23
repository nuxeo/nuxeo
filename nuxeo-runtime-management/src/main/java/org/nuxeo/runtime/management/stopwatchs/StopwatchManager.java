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
package org.nuxeo.runtime.management.stopwatchs;

import org.nuxeo.runtime.management.metrics.MetricAttributesProvider;
import org.nuxeo.runtime.management.metrics.MetricHistoryStack;


/**
 * Service interface to manage Counters.
 *
 * This services hides the Stopwatch implementation so that Stopwatch's updated
 * don't have to be dependent on Simon
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public interface StopwatchManager extends MetricAttributesProvider {

    public interface Split {
        String getName();
        void stop();
    }

    /**
     * Acquire a split
     *
     * @param name
     * @return
     */
    Split start(String name);

    /**
     * Watch history
     *
     * @param name
     * @return
     */
    MetricHistoryStack getStack(String name);

    /**
     * Enables all
     *
     */
    void enableStopwatchs();

    /**
     * Disable all
     *
     */
    void disableStopwatchs();

}
