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

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;

/**
 *
 * @since TODO
 */
public class DummyPipe implements EventBundlePipe {

    public static List<Event> receivedEvents = new ArrayList<>();

    @Override
    public void initPipe(String name, Map<String, String> params) {

    }

    @Override
    public void sendEventBundle(EventBundle events) {
        for ( Event event : events) {
            receivedEvents.add(event);
        }
    }

    @Override
    public boolean waitForCompletion(long timeoutMillis) throws InterruptedException {
        return true;
    }

    @Override
    public void shutdown() throws InterruptedException {
    }


}
