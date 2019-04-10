/*
 * (C) Copyright 2012-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.platform.routing.test;

import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * This feature provides the basic deployments needed to run a test that uses Document Routing.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 */
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.task.core:OSGI-INF/task-core-types-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.task.core:OSGI-INF/TaskService.xml")
@Deploy("org.nuxeo.ecm.platform.task.core:OSGI-INF/task-adapter-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.routing.core:OSGI-INF/document-routing-service.xml")
@Deploy("org.nuxeo.ecm.platform.routing.core:OSGI-INF/document-routing-persister-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.routing.core:OSGI-INF/document-routing-core-types-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.routing.core:OSGI-INF/document-routing-adapter-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.routing.core:OSGI-INF/document-routing-life-cycle-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.routing.core:OSGI-INF/document-routing-engine-service.xml")
@Deploy("org.nuxeo.ecm.platform.routing.core:OSGI-INF/document-routing-operations-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.filemanager.core")
@Deploy("org.nuxeo.ecm.core.mimetype:OSGI-INF/nxmimetype-service.xml")
public class DocumentRoutingFeature implements RunnerFeature {

}
