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
 *     Stephane Lacoin
 */
package org.nuxeo.ecm.platform.publisher.task;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.runtime.api.Framework;

class LookupStateByTask implements LookupState {

    @Override
    public boolean isPublished(DocumentModel doc, CoreSession session) {
        List<Task> tasks = Framework.getService(TaskService.class).getTaskInstances(doc, (NuxeoPrincipal) null,
                session);
        for (Task task : tasks) {
            if (task.getName().equals(CoreProxyWithWorkflowFactory.TASK_NAME)) {
                // if there is a task on this doc, then it is not yet
                // published
                return false;
            }
        }
        return true;
    }
}
