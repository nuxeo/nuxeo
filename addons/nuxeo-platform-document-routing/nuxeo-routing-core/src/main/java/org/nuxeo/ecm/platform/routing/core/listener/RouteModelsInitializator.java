/*
 * (C) Copyright 2009-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *    Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.core.listener;

import java.time.Duration;

import org.nuxeo.common.utils.DurationUtils;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.repository.RepositoryInitializationHandler;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.ClusterLockHelper;

/**
 * Imports route models in the root folder defined by the current persister from a contributed zip resource. Uses the IO
 * core service, through @{link FileManager}
 *
 * @since 5.6
 */
public class RouteModelsInitializator extends RepositoryInitializationHandler {

    /** @since 11.5 */
    public static final String CLUSTER_START_DURATION_PROP = "org.nuxeo.workflow.cluster.start.duration";

    /** @since 11.5 */
    public static final Duration CLUSTER_START_DURATION_DEFAULT = Duration.ofMinutes(1);

    @Override
    public void doInitializeRepository(CoreSession session) {
        String repositoryName = session.getRepositoryName();
        String prop = Framework.getProperty(CLUSTER_START_DURATION_PROP);
        Duration duration = DurationUtils.parsePositive(prop, CLUSTER_START_DURATION_DEFAULT);
        Duration pollDelay = Duration.ofSeconds(1);
        ClusterLockHelper.runAtomically("start-workflows-" + repositoryName, duration, pollDelay,
                () -> importWorkflows(repositoryName));
    }

    protected void importWorkflows(String repositoryName) {
        DocumentRoutingService service = Framework.getService(DocumentRoutingService.class);
        CoreInstance.doPrivileged(repositoryName, session -> {
            service.importAllRouteModels(session);
            session.save();
        });
    }

}
