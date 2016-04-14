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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.work;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.work.api.WorkQueueDescriptor;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for the {@link WorkQueueDescriptor}s.
 *
 * @since 5.6
 */
public class WorkQueueRegistry extends ContributionFragmentRegistry<WorkQueueDescriptor> {

    private static final Log log = LogFactory.getLog(WorkQueueRegistry.class);

    protected Map<String, WorkQueueDescriptor> registry = new HashMap<String, WorkQueueDescriptor>();

    protected Map<String, String> categoryToQueueId = new HashMap<String, String>();

    /**
     * Gets the descriptor for a given queue id.
     *
     * @param queueId the queue id
     * @return the queue descriptor, or {@code null}
     */
    public WorkQueueDescriptor get(String queueId) {
        return registry.get(queueId);
    }

    /**
     * Gets the list of queue ids.
     *
     * @return the list of queue ids
     */
    public List<String> getQueueIds() {
        return new ArrayList<String>(registry.keySet());
    }

    @Override
    public String getContributionId(WorkQueueDescriptor contrib) {
        return contrib.id;
    }

    @Override
    public void contributionUpdated(String id, WorkQueueDescriptor contrib, WorkQueueDescriptor newOrigContrib) {
        registry.put(id, contrib);
    }

    @Override
    public void contributionRemoved(String id, WorkQueueDescriptor origContrib) {
        registry.remove(id);
    }

    protected void index() {
        for (Entry<String, WorkQueueDescriptor> es : registry.entrySet()) {
            String queueId = es.getKey();
            for (String category : es.getValue().categories) {
                String old = categoryToQueueId.get("category");
                if (old != null) {
                    log.error("Work category '" + category + "' cannot be assigned to work queue '" + queueId
                            + "' because it is already assigned to work queue '" + old + "'");
                } else {
                    categoryToQueueId.put(category, queueId);
                }
            }
        }
    }

    public String getQueueId(String category) {
        return categoryToQueueId.get(category);
    }

    @Override
    public WorkQueueDescriptor clone(WorkQueueDescriptor orig) {
        return orig.clone();
    }

    @Override
    public void merge(WorkQueueDescriptor src, WorkQueueDescriptor dst) {
        dst.merge(src);
    }

}
