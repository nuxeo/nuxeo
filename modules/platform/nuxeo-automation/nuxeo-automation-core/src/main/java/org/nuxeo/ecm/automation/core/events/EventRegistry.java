/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.automation.core.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * @since 5.6
 */
public class EventRegistry extends ContributionFragmentRegistry<EventHandler> {

    private static final Logger log = LogManager.getLogger(EventRegistry.class);

    protected volatile Map<String, List<EventHandler>> lookup;

    @Override
    public String getContributionId(EventHandler contrib) {
        String id = contrib.getId();
        if (id == null) {
            id = contrib.chainId;
            if (contrib.events != null && !contrib.events.isEmpty()) {
                id += "_" + String.join("_", contrib.events);
            }
            log.debug("An EventHandler without id has been contributed. Generated id: {} ", id);
        }
        return id;
    }

    @Override
    public void contributionUpdated(String id, EventHandler handler, EventHandler newOrigContrib) {
        lookup = null;
    }

    @Override
    public void contributionRemoved(String id, EventHandler handler) {
        lookup = null;
    }

    @Override
    public EventHandler clone(EventHandler orig) {
        return orig.clone();
    }

    @Override
    public void merge(EventHandler src, EventHandler dst) {
        dst.merge(src);
    }

    // API

    public Map<String, List<EventHandler>> lookup() {
        if (lookup == null) {
            synchronized (this) {
                if (lookup == null) {
                    lookup = new HashMap<>();
                    for (var eventHandler : toMap().values()) {
                        if (eventHandler.isEnabled()) {
                            for (var eventId : eventHandler.getEvents()) {
                                lookup.computeIfAbsent(eventId, k -> new ArrayList<>()).add(eventHandler);
                            }
                        }
                    }
                }
            }
        }
        return lookup;
    }

}
