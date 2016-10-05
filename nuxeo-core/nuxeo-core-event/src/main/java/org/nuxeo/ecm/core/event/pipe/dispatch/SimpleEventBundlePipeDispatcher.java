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

import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.pipe.EventBundlePipe;
import org.nuxeo.ecm.core.event.pipe.EventPipeDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Basic implementation that simply forwards {@link EventBundle} to all underlying {@link EventBundlePipe}
 *
 * @since 8.4
 */
public class SimpleEventBundlePipeDispatcher implements EventBundleDispatcher {

    protected List<EventBundlePipe> pipes = new ArrayList<EventBundlePipe>();

    protected Map<String, String> parameters;

    @Override
    public void init(List<EventPipeDescriptor> pipeDescriptors,  Map<String, String> parameters) {

        this.parameters = parameters;

        pipeDescriptors.sort((o1, o2) -> o1.getPriority().compareTo(o2.getPriority()));

        for (EventPipeDescriptor descriptor : pipeDescriptors) {
            EventBundlePipe pipe = descriptor.getInstance();
            pipe.initPipe(descriptor.getName(), descriptor.getParameters());
            pipes.add(pipe);
        }
    }

    @Override
    public void sendEventBundle(EventBundle events) {
        if (events.size() == 0) {
            return;
        }
        for (EventBundlePipe pipe : pipes) {
            pipe.sendEventBundle(events);
        }
    }

    @Override
    public boolean waitForCompletion(long timeoutMillis) throws InterruptedException {
        boolean res = true;
        for (EventBundlePipe pipe : pipes) {
            res = res && pipe.waitForCompletion(timeoutMillis);
        }
        return res;
    }

    @Override
    public void shutdown() throws InterruptedException {
        for (EventBundlePipe pipe : pipes) {
            pipe.shutdown();
        }
    }
}
