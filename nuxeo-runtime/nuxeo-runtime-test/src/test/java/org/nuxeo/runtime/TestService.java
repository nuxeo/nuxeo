/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime;

import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.nuxeo.runtime.services.deployment.DeploymentService;
import org.nuxeo.runtime.services.event.EventService;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestService extends NXRuntimeTestCase {

    @Test
    public void testServiceLookup() {
        EventService eventComponent = (EventService) runtime.getComponent(EventService.NAME);
        EventService eventService = runtime.getService(EventService.class);
        assertSame(eventComponent, eventService);

        DeploymentService deploymentComponent = (DeploymentService) runtime.getComponent(DeploymentService.NAME);
        DeploymentService deploymentService = runtime.getService(DeploymentService.class);
        assertSame(deploymentComponent, deploymentService);
    }

}
