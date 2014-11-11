package org.nuxeo.ecm.platform.publisher.task;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.runtime.api.Framework;

class LookupStateByTask implements LookupState {

    @Override
    public boolean isPublished(DocumentModel doc, CoreSession session)
            throws ClientException {
        List<Task> tasks = Framework.getLocalService(TaskService.class).getTaskInstances(
                doc, (NuxeoPrincipal) null, session);
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