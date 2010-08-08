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
package org.nuxeo.ecm.core.event.jmx;

/**
 * Simple class to store Listeners call statistics.
 *
 * @author Thierry Delprat
 */
public class CallStat {

    long accumulatedTime = 0;

    int callCount = 0;

    final String label;

    public CallStat(String label) {
        this.label = label;
    }

    void update(long delta) {
        callCount++;
        accumulatedTime += delta;
    }

    public long getAccumulatedTime() {
        return accumulatedTime;
    }

    public int getCallCount() {
        return callCount;
    }

    public String getLabel() {
        return label;
    }

}
