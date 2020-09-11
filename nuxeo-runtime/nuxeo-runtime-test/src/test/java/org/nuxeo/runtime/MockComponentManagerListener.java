/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Mock component registering itself to component manager.
 *
 * @since 11.3
 */
public class MockComponentManagerListener extends DefaultComponent implements ComponentListener {

    public static final String NAME = "component.manager.listener";

    protected List<ComponentEvent> events = new ArrayList<>();

    public MockComponentManagerListener() {
        Framework.getRuntime().getComponentManager().addComponentListener(this);
    }

    @Override
    public void handleEvent(ComponentEvent event) {
        this.events.add(event);
    }

    public List<ComponentEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public boolean hasEvent(int event, String component) {
        return events.stream().anyMatch(e -> event == e.id && component.equals(e.registrationInfo.getName().getName()));
    }

}
