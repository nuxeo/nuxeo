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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.storage.sql.RepositoryManagement;
import org.nuxeo.ecm.core.storage.sql.RepositoryResolver;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * @since 5.8
 *
 */
public class DocumentRoutingWorkflowInstancesCleanup implements EventListener {

    public final static String CLEANUP_WORKFLOW_INSTANCES_PROPERTY = "nuxeo.routing.disable.cleanup.workflow.instances";

    public final static String CLEANUP_WORKFLOW_INSTANCES_BATCH_SIZE_PROPERTY = "nuxeo.routing.cleanup.workflow.instances.batch.size";

    @Override
    public void handleEvent(Event event) throws ClientException {
        if (!"workflowInstancesCleanup".equals(event.getName())
                || Framework.isBooleanPropertyTrue(CLEANUP_WORKFLOW_INSTANCES_PROPERTY)) {
            return;
        }

        int batchSize = Integer.parseInt(Framework.getProperty(
                CLEANUP_WORKFLOW_INSTANCES_BATCH_SIZE_PROPERTY, "100"));
        for (RepositoryManagement repoMgmt : RepositoryResolver.getRepositories()) {
            Framework.getLocalService(DocumentRoutingService.class).cleanupDoneAndCanceledRouteInstances(
                    repoMgmt.getName(), batchSize);
        }
    }
}