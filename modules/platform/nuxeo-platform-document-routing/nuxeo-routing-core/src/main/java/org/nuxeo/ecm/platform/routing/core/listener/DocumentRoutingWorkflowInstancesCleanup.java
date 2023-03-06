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

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.8
 */
public class DocumentRoutingWorkflowInstancesCleanup implements PostCommitEventListener {

    public final static String CLEANUP_WORKFLOW_INSTANCES_PROPERTY = "nuxeo.routing.disable.cleanup.workflow.instances";

    public final static String CLEANUP_WORKFLOW_INSTANCES_BATCH_SIZE_PROPERTY = "nuxeo.routing.cleanup.workflow.instances.batch.size";

    public final static String CLEANUP_WORKFLOW_INSTANCES_ORPHAN_PROPERTY = "nuxeo.routing.cleanup.workflow.instances.orphan";

    public final static String CLEANUP_WORKFLOW_REPO_NAME_PROPERTY = "repositoryName";

    public final static String CLEANUP_WORKFLOW_EVENT_NAME = "workflowInstancesCleanup";

    @Override
    public void handleEvent(EventBundle events) {
        if (Framework.isBooleanPropertyTrue(CLEANUP_WORKFLOW_INSTANCES_PROPERTY)) {
            return;
        }
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        DocumentRoutingService routingService = Framework.getService(DocumentRoutingService.class);
        Set<String> repositoryNames = new HashSet<>();
        for (Event event : events) {
            if (event.getContext().hasProperty(CLEANUP_WORKFLOW_REPO_NAME_PROPERTY)) {
                String repositoryName = (String) event.getContext().getProperty(CLEANUP_WORKFLOW_REPO_NAME_PROPERTY);
                repositoryNames.add(repositoryName);
            } else {
                repositoryNames.addAll(repositoryManager.getRepositoryNames());
            }
        }
        repositoryNames.forEach(repo -> routingService.cleanupRouteInstances(repo));
    }

}
