/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
public class WorkQueueDescriptorRegistry extends
        ContributionFragmentRegistry<WorkQueueDescriptor> {

    private static final Log log = LogFactory.getLog(WorkQueueDescriptorRegistry.class);

    protected final WorkManagerImpl works;

    protected Map<String, WorkQueueDescriptor> registry = new HashMap<String, WorkQueueDescriptor>();

    protected volatile boolean refresh;

    protected Map<String, String> categoryToQueueId = new HashMap<String, String>();

    public WorkQueueDescriptorRegistry(WorkManagerImpl works) {
        this.works = works;
    }

    /**
     * Gets the descriptor for a given queue id.
     *
     * @param queueId the queue id
     * @return the queue descriptor, or {@code null}
     */
    public synchronized WorkQueueDescriptor get(String queueId) {
        return registry.get(queueId);
    }

    /**
     * Gets the list of queue ids.
     *
     * @return the list of queue ids
     */
    public synchronized List<String> getQueueIds() {
        return new ArrayList<String>(registry.keySet());
    }

    @Override
    public String getContributionId(WorkQueueDescriptor contrib) {
        return contrib.id;
    }

    @Override
    public void contributionUpdated(String id, WorkQueueDescriptor contrib,
            WorkQueueDescriptor newOrigContrib) {
        registry.put(id, contrib);
        refresh = true;
        if (works.started) {
            works.activateQueue(contrib);
        }
    }

    @Override
    public void contributionRemoved(String id, WorkQueueDescriptor origContrib) {
        if (works.started) {
            works.deactivateQueue(origContrib);
        }
        registry.remove(id);
        refresh = true;
    }

    protected synchronized void refresh() {
        for (Entry<String, WorkQueueDescriptor> es : registry.entrySet()) {
            String queueId = es.getKey();
            for (String category : es.getValue().categories) {
                String old = categoryToQueueId.get("category");
                if (old != null) {
                    log.error("Work category '"
                            + category
                            + "' cannot be assigned to work queue '"
                            + queueId
                            + "' because it is already assigned to work queue '"
                            + old + "'");
                } else {
                    categoryToQueueId.put(category, queueId);
                }
            }
        }
    }

    public String getQueueId(String category) {
        if (refresh) {
            refresh();
        }
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
