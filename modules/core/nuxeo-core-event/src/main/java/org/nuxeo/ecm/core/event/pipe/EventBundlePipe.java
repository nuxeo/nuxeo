/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tiry
 */
package org.nuxeo.ecm.core.event.pipe;

import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventService;

import java.util.Map;

/**
 * Interface for a pipe of events. This is the abstraction used to bridge the Nuxeo events to pipes that consume them.
 *
 * @since 8.4
 */
public interface EventBundlePipe {

    /**
     * Initialize the Pipe when Nuxeo Event Service starts
     *
     * @param name the name as defined in the XMap descriptor
     * @param params the parameters as defined in the XMap descriptor
     */
    void initPipe(String name, Map<String, String> params);

    /**
     * Send an {@link EventBundle} inside the pipe
     */
    void sendEventBundle(EventBundle events);

    /**
     * Wait until the end of event consumption
     */
    boolean waitForCompletion(long timeoutMillis) throws InterruptedException;

    /**
     * Shutdown callback when the {@link EventService} stops
     */
    void shutdown() throws InterruptedException;
}
