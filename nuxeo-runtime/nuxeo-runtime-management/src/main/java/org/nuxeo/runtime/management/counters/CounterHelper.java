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

import org.nuxeo.runtime.api.Framework;

/**
 * Dummy helper class to be used by code that updates the counters to avoid
 * having to do the service lookup
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class CounterHelper {

    protected static CounterManager cm = Framework.getLocalService(CounterManager.class);

    public static void increaseCounter(String counterName) {
        if (cm!=null) {
            cm.increaseCounter(counterName);
        }
    }

    public static void increaseCounter(String counterName, long value) {
        if (cm!=null) {
            cm.increaseCounter(counterName, value);
        }
    }

    public static void setCounterValue(String counterName, long value) {
        if (cm!=null) {
            cm.setCounterValue(counterName, value);
        }
    }

    public static void decreaseCounter(String counterName) {
        if (cm!=null) {
            cm.decreaseCounter(counterName);
        }
    }

    public static void decreaseCounter(String counterName, long value) {
        if (cm!=null) {
            cm.decreaseCounter(counterName, value);
        }
    }

}
