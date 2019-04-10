/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.core.listener;

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.8
 */
public class DocumentRoutingWorkflowInstancesCleanup implements PostCommitEventListener {

    public final static String CLEANUP_WORKFLOW_INSTANCES_PROPERTY = "nuxeo.routing.disable.cleanup.workflow.instances";

    public final static String CLEANUP_WORKFLOW_INSTANCES_BATCH_SIZE_PROPERTY = "nuxeo.routing.cleanup.workflow.instances.batch.size";

    public final static String CLEANUP_WORKFLOW_REPO_NAME_PROPERTY = "repositoryName";

    public final static String CLEANUP_WORKFLOW_EVENT_NAME = "workflowInstancesCleanup";

    @Override
    public void handleEvent(EventBundle events) {
        int batchSize = Integer.parseInt(
                Framework.getProperty(CLEANUP_WORKFLOW_INSTANCES_BATCH_SIZE_PROPERTY, "100"));
        DocumentRoutingService routing = Framework.getService(DocumentRoutingService.class);
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);

        Set<String> repositoryNames = new HashSet<>();
        for (Event event : events) {
            if (event.getContext().hasProperty(CLEANUP_WORKFLOW_REPO_NAME_PROPERTY)) {
                String repositoryName = (String) event.getContext().getProperty(CLEANUP_WORKFLOW_REPO_NAME_PROPERTY);
                repositoryNames.add(repositoryName);
            } else {
                repositoryNames.addAll(repositoryManager.getRepositoryNames());
            }
        }
        for (String repositoryName : repositoryNames) {
            doCleanAndReschedule(batchSize, routing, repositoryName);
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
