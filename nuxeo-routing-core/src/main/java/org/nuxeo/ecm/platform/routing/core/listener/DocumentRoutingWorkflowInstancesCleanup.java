/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.core.listener;

import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.8
 */
public class DocumentRoutingWorkflowInstancesCleanup implements EventListener {

    public final static String CLEANUP_WORKFLOW_INSTANCES_PROPERTY = "nuxeo.routing.disable.cleanup.workflow.instances";

    public final static String CLEANUP_WORKFLOW_INSTANCES_BATCH_SIZE_PROPERTY = "nuxeo.routing.cleanup.workflow.instances.batch.size";

    public final static String CLEANUP_WORKFLOW_REPO_NAME_PROPERTY = "repositoryName";

    public final static String CLEANUP_WORKFLOW_EVENT_NAME = "workflowInstancesCleanup";

    @Override
    public void handleEvent(Event event) {
        if (!CLEANUP_WORKFLOW_EVENT_NAME.equals(event.getName())
                || Framework.isBooleanPropertyTrue(CLEANUP_WORKFLOW_INSTANCES_PROPERTY)) {
            return;
        }

        int batchSize = Integer.parseInt(Framework.getProperty(CLEANUP_WORKFLOW_INSTANCES_BATCH_SIZE_PROPERTY, "100"));
        DocumentRoutingService routing = Framework.getLocalService(DocumentRoutingService.class);
        RepositoryManager repositoryManager = Framework.getLocalService(RepositoryManager.class);

        if (event.getContext().hasProperty(CLEANUP_WORKFLOW_REPO_NAME_PROPERTY)) {
            doCleanAndReschedule(batchSize, routing,
                    event.getContext().getProperty(CLEANUP_WORKFLOW_REPO_NAME_PROPERTY).toString());
        } else {
            for (String repositoryName : repositoryManager.getRepositoryNames()) {
                doCleanAndReschedule(batchSize, routing, repositoryName);
            }
        }
    }

    /**
     * @since 7.1
     */
    private void doCleanAndReschedule(int batchSize, DocumentRoutingService routing, String repositoryName) {
        int cleanedUpWf = routing.doCleanupDoneAndCanceledRouteInstances(repositoryName, batchSize);
        if (cleanedUpWf == batchSize) {
            EventContextImpl eCtx = new EventContextImpl();
            eCtx.setProperty(CLEANUP_WORKFLOW_REPO_NAME_PROPERTY, repositoryName);
            Event event = eCtx.newEvent(CLEANUP_WORKFLOW_EVENT_NAME);
            EventProducer eventProducer = Framework.getService(EventProducer.class);
            eventProducer.fireEvent(event);
        }
    }

}
