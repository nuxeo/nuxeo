/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     tiry
 */
package org.nuxeo.ecm.core.event.pipe.dispatch;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.pipe.EventBundlePipe;
import org.nuxeo.ecm.core.event.pipe.EventPipeDescriptor;

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
    public void init(List<EventPipeDescriptor> pipeDescriptors, Map<String, String> parameters);

    /**
     * Forward an {@link EventBundle} to the underlying {@link EventBundlePipe}s
     *
     * @param events
     */
    public void sendEventBundle(EventBundle events);

    /**
     * Wait until the end of processing
     *
     * @param timeoutMillis
     * @return
     * @throws InterruptedException
     */
    public boolean waitForCompletion(long timeoutMillis) throws InterruptedException;

    /**
     * Shutdown callback
     *
     * @throws InterruptedException
     */
    public void shutdown() throws InterruptedException;

}