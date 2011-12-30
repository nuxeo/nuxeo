package org.nuxeo.ecm.platform.publisher.jbpm;

import java.util.List;

import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.runtime.api.Framework;

public class LookupStateByTask implements LookupState {

    @Override
    public boolean isPublished(DocumentModel doc, CoreSession session)
            throws ClientException {
        List<TaskInstance> tis = Framework.getLocalService(JbpmService.class).getTaskInstances(doc,
                (NuxeoPrincipal) null, null);
        for (TaskInstance ti : tis) {
            if (ti.getName().equals(CoreProxyWithWorkflowFactory.TASK_NAME)) {
                // if there is a task on this doc, then it is not yet
                // published
                return false;
            }
        }
        return true;
    }
}