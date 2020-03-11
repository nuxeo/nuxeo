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
 * $Id$
 */
package org.nuxeo.runtime.management.counters;

import org.nuxeo.runtime.api.Framework;

/**
 * Dummy helper class to be used by code that updates the counters to avoid having to do the service lookup
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class CounterHelper {

    protected static CounterManager cm = Framework.getService(CounterManager.class);

    public static void increaseCounter(String counterName) {
        if (cm != null) {
            cm.increaseCounter(counterName);
        }
    }

    public static void increaseCounter(String counterName, long value) {
        if (cm != null) {
            cm.increaseCounter(counterName, value);
        }
    }

    public static void setCounterValue(String counterName, long value) {
        if (cm != null) {
            cm.setCounterValue(counterName, value);
        }
    }

    public static void decreaseCounter(String counterName) {
        if (cm != null) {
            cm.decreaseCounter(counterName);
        }
    }

    public static void decreaseCounter(String counterName, long value) {
        if (cm != null) {
            cm.decreaseCounter(counterName, value);
        }
    }

}
