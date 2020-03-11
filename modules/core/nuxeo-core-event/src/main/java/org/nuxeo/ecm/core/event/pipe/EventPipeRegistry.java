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

import org.nuxeo.runtime.model.ContributionFragmentRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contribution registry for EventPipeDescriptor
 *
 * @since 8.4
 */
public class EventPipeRegistry extends ContributionFragmentRegistry<EventPipeDescriptor> {

    protected Map<String, EventPipeDescriptor> reg = new HashMap<>();

    @Override
    public EventPipeDescriptor clone(EventPipeDescriptor desc) {
        return desc.clone();
    }

    @Override
    public void contributionRemoved(String name, EventPipeDescriptor desc) {
        reg.remove(name);
    }

    @Override
    public void contributionUpdated(String name, EventPipeDescriptor desc, EventPipeDescriptor origin) {
        reg.put(name, desc);
    }

    @Override
    public String getContributionId(EventPipeDescriptor desc) {
        return desc.getName();
    }

    @Override
    public void merge(EventPipeDescriptor src, EventPipeDescriptor dest) {
        dest.merge(src);
    }

    public EventPipeDescriptor getPipeDescriptor(String name) {
        return reg.get(name);
    }

    public Set<String> getPipeNames() {
        return reg.keySet();
    }

    public List<EventPipeDescriptor> getPipes() {
        return new ArrayList<>(reg.values());
    }


}
