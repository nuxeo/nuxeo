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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.platform.routing.test;

import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.SimpleFeature;

/**
 * This feature provides the basic deployments needed to run a test that uses
 * Document Routing.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 */
@Features(CoreFeature.class)
@Deploy({
        "org.nuxeo.ecm.platform.task.core:OSGI-INF/task-core-types-contrib.xml",
        "org.nuxeo.ecm.platform.task.core:OSGI-INF/TaskService.xml",
        "org.nuxeo.ecm.platform.task.core:OSGI-INF/task-adapter-contrib.xml",
        "org.nuxeo.ecm.platform.routing.core:OSGI-INF/document-routing-service.xml",
        "org.nuxeo.ecm.platform.routing.core:OSGI-INF/document-routing-persister-contrib.xml",
        "org.nuxeo.ecm.platform.routing.core:OSGI-INF/document-routing-core-types-contrib.xml",
        "org.nuxeo.ecm.platform.routing.core:OSGI-INF/document-routing-adapter-contrib.xml",
        "org.nuxeo.ecm.platform.routing.core:OSGI-INF/document-routing-life-cycle-contrib.xml",
        "org.nuxeo.ecm.platform.routing.core:OSGI-INF/document-routing-engine-service.xml",
        "org.nuxeo.ecm.platform.routing.core:OSGI-INF/document-routing-operations-contrib.xml",
        "org.nuxeo.ecm.platform.routing.core:OSGI-INF/document-routing-task-service.xml",
        "org.nuxeo.ecm.platform.filemanager.core",
        "org.nuxeo.ecm.platform.mimetype.core:OSGI-INF/nxmimetype-service.xml" })
public class DocumentRoutingFeature extends SimpleFeature {

}
