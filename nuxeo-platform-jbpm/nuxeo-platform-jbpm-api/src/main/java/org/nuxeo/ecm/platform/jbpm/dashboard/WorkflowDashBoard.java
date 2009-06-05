package org.nuxeo.ecm.platform.jbpm.dashboard;

import java.util.Collection;

import org.nuxeo.ecm.core.api.ClientException;

public interface WorkflowDashBoard {

    public Collection<DashBoardItem> computeDashboardItems()
            throws ClientException;

    public Collection<DocumentProcessItem> computeDocumentProcessItems()
            throws ClientException;

    public void invalidateDocumentProcessItems();

    public void invalidateDashboardItems();

    public String refreshDashboardItems();

    public String refreshDocumentProcessItems();

}