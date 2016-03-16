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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.pipe.EventBundlePipe;
import org.nuxeo.ecm.core.event.pipe.EventPipeDescriptor;

/**
 *
 * @since TODO
 */
public class EventBundlePipeDispatcher{

    protected List<EventBundlePipe> pipes = new ArrayList<EventBundlePipe>();

    public void init(List<EventPipeDescriptor> pipeDescriptors) {

        pipeDescriptors.sort(new Comparator<EventPipeDescriptor>() {
            @Override
            public int compare(EventPipeDescriptor o1, EventPipeDescriptor o2) {

                return o1.getPriority().compareTo(o2.getPriority());
            }
        });

        for (EventPipeDescriptor descriptor : pipeDescriptors) {
            EventBundlePipe pipe = descriptor.getInstance();
            pipe.initPipe(descriptor.getName(), descriptor.getParameters());
            pipes.add(pipe);
        }
    }

    public void sendEventBundle(EventBundle events) {
        if (events.size() == 0) {
            return;
        }
        for (EventBundlePipe pipe: pipes) {
            pipe.sendEventBundle(events);
        }
    }
}
