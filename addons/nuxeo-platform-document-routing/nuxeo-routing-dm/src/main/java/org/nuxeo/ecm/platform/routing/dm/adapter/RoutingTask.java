/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     ldoguin
 */
package org.nuxeo.ecm.platform.routing.dm.adapter;

import org.nuxeo.ecm.platform.routing.api.ActionableObject;
import org.nuxeo.ecm.platform.task.Task;

/**
 * @deprecated since 5.9.2 - Use only routes of type 'graph' The facet 'RoutingTask' is still used to mark tasks created
 *             by the workflow, but it this class is marked as deprecated as it extends the deprecated ActionableObject
 */
@Deprecated
public interface RoutingTask extends Task, ActionableObject {

}
