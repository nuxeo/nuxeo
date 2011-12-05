package org.nuxeo.ecm.platform.task;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;

public class TaskAdapterFactory implements DocumentAdapterFactory{

    @Override
    public Object getAdapter(DocumentModel doc, Class<?> itf) {
        if (doc.hasFacet(TaskConstants.TASK_FACET_NAME)) {
            return new TaskImpl(doc);
        } else {
            return null;
        }

    }

}
