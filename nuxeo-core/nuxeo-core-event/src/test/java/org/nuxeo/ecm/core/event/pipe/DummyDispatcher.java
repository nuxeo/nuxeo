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
        eventBundles = new ArrayList<>();
    }

    @Override
    public void sendEventBundle(EventBundle events) {
        DummyDispatcher.eventBundles.add(events);
        super.sendEventBundle(events);
    }

}
