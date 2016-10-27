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

import java.util.ArrayList;
import java.util.List;

/**
 * Simple stacking descriptor registry
 *
 * @since 8.4
 */
public class EventDispatcherRegistry {

    protected List<EventDispatcherDescriptor> contribs = new ArrayList<>();

    public void addContrib(EventDispatcherDescriptor contrib) {
        contribs.add(contrib);
    }

    public void removeContrib(EventDispatcherDescriptor contrib) {
        contribs.removeIf(descriptor -> descriptor.getName().equals(contrib.getName()));
    }

    public EventDispatcherDescriptor getDispatcherDescriptor() {
        return contribs.isEmpty() ? null : contribs.get(contribs.size() - 1);
    }
}
