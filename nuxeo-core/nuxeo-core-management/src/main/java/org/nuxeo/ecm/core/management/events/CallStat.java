/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.core.management.events;

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
