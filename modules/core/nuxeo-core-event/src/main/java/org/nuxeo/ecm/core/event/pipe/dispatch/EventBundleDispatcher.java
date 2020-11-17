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
package org.nuxeo.ecm.core.event.pipe.dispatch;

import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.pipe.EventBundlePipe;
import org.nuxeo.ecm.core.event.pipe.EventPipeDescriptor;

import java.util.List;
import java.util.Map;

/**
 * Interface for dispatching {@link EventBundle} between different {@link EventBundlePipe}
 *
 * @since 8.4
 */
public interface EventBundleDispatcher {

    /**
     * Initialize the dispatcher
     *
     * @param pipeDescriptors descriptors of the underlying {@link EventBundlePipe}s
     */
    void init(List<EventPipeDescriptor> pipeDescriptors, Map<String, String> parameters);

    /**
     * Forward an {@link EventBundle} to the underlying {@link EventBundlePipe}s
     */
    void sendEventBundle(EventBundle events);

    /**
     * Wait until the end of processing
     */
    boolean waitForCompletion(long timeoutMillis) throws InterruptedException;

    /**
     * Shutdown callback
     */
    void shutdown() throws InterruptedException;

}
