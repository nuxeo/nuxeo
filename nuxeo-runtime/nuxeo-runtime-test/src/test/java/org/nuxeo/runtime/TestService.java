/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime;

import org.nuxeo.runtime.services.deployment.DeploymentService;
import org.nuxeo.runtime.services.event.EventService;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestService extends NXRuntimeTestCase {

    public void testServiceLookup() {
        EventService eventComponent = (EventService) runtime.getComponent(EventService.NAME);
        EventService eventService = runtime.getService(EventService.class);
        assertSame(eventComponent, eventService);

        DeploymentService deploymentComponent = (DeploymentService) runtime.getComponent(DeploymentService.NAME);
        DeploymentService deploymentService = runtime.getService(DeploymentService.class);
        assertSame(deploymentComponent, deploymentService);
    }

}
