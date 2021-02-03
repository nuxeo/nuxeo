/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.work.api;

import static org.nuxeo.ecm.core.work.api.WorkQueueDescriptor.ALL_QUEUES;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.w3c.dom.Element;

/**
 * Specific registry to handle the "*" wild card to enable/disable processing on a group of contributions.
 *
 * @since 11.5
 */
public class WorkQueueRegistry extends MapRegistry {

    private static final Logger log = LogManager.getLogger(WorkQueueRegistry.class);

    @Override
    protected <T> T doRegister(Context ctx, XAnnotatedObject xObject, Element element, String extensionId) {
        String id = computeId(ctx, xObject, element);
        if (WorkQueueDescriptor.ALL_QUEUES.equals(id)) {
            // impact existing descriptors
            WorkQueueDescriptor allDesc = (WorkQueueDescriptor) xObject.newInstance(ctx, element);
            Boolean processing = allDesc.processing;
            if (processing == null) {
                log.error("Ignoring work queue descriptor {} with no processing/queuing", ALL_QUEUES);
            } else {
                log.info("Setting on all work queues: processing={}", processing);
                contributions.values().forEach(desc -> ((WorkQueueDescriptor) desc).processing = processing);
            }
            return null;
        } else {
            return super.doRegister(ctx, xObject, element, extensionId);
        }
    }

}
