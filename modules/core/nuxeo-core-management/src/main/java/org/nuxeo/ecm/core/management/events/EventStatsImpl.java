/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.core.management.events;

import org.nuxeo.ecm.core.event.EventStats;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated since 11.4: superseded by dropwizard metrics
 */
@Deprecated(since = "11.4")
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
