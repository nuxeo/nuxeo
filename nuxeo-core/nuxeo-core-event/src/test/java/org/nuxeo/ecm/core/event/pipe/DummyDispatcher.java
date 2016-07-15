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
package org.nuxeo.ecm.core.event.pipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.pipe.dispatch.SimpleEventBundlePipeDispatcher;

/**
 *
 * @since TODO
 */
public class DummyDispatcher extends SimpleEventBundlePipeDispatcher  {

    protected static List<EventPipeDescriptor> pipeDescriptors;

    protected static List<EventBundle> eventBundles;

    @Override
    public void init(List<EventPipeDescriptor> pipeDescriptors, Map<String, String> parameters) {
        super.init(pipeDescriptors, parameters);
        DummyDispatcher.pipeDescriptors = pipeDescriptors;
        eventBundles = new ArrayList<EventBundle>();
    }

    @Override
    public void sendEventBundle(EventBundle events) {
        DummyDispatcher.eventBundles.add(events);
        super.sendEventBundle(events);
    }

}
