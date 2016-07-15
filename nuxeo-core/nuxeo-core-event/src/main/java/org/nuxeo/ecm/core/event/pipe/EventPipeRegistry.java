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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Contribution registry for EventPipeDescriptor
 *
 * @since 8.4
 */
public class EventPipeRegistry extends ContributionFragmentRegistry<EventPipeDescriptor> {

    protected Map<String, EventPipeDescriptor> reg = new HashMap<String, EventPipeDescriptor>();

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
        return new ArrayList<EventPipeDescriptor>(reg.values());
    }


}
