/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.core.management.events;

import org.nuxeo.ecm.core.event.EventStats;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class EventStatsImpl implements EventStats {

    @Override
    public void logAsyncExec(EventListenerDescriptor desc, long delta) {
        EventStatsHolder.logAsyncExec(desc, delta);
    }

    @Override
    public void logSyncExec(EventListenerDescriptor desc, long delta) {
        EventStatsHolder.logSyncExec(desc, delta);
    }

}
