/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.workflow.document.ejb;

import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;

import org.nuxeo.ecm.platform.workflow.api.client.delegate.WAPIBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.*;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl.WMParticipantImpl;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.WorkItemsListsBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.document.api.workitem.WorkItemsListEntry;
import org.nuxeo.ecm.platform.workflow.document.api.workitem.WorkItemsListsManager;
import org.nuxeo.ecm.platform.workflow.document.api.workitem.WorkItemsListException;

/**
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class TestWorkItemsListsBeanRemote extends TestCase {

    private static final String sampleParticipantName = "Cartman";

    private static final String sampleProcessDefinitionName = "document_review_parallel";

    // Sample with 2 workitems on the activity instance
    private String samplePid;

    // Process with no work items
    private String samplePidWithNoWorkItem;

    private String sampleProcessWorkItemDefinitionId;

    private WorkItemsListsManager wiListsBean;

    private WAPI wapiBean;

    private final WorkItemsListsBusinessDelegate wiListsBD = new WorkItemsListsBusinessDelegate();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        wiListsBean = wiListsBD.getWorkItemLists();
        wapiBean = WAPIBusinessDelegate.getWAPI();
        assertNotNull(wiListsBean);
        assertNotNull(wapiBean);
        startSampleProc();
    }

    @Override
    public void tearDown() throws Exception {
        killSampleProc();
        wiListsBean = null;
        wapiBean = null;
        super.tearDown();
    }

    public void xtestEmptyWorkItemsLists() throws WorkItemsListException {
        List<WorkItemsListEntry> entries;

        entries = wiListsBean.getWorkItemListsFor("fake", "fake");
        assertEquals(0, entries.size());

        entries = wiListsBean.getWorkItemListsFor(null, null);
        assertEquals(0, entries.size());
    }

    public void testWorkItemsListCreation() throws Exception {
        List<WorkItemsListEntry> entries = wiListsBean.getWorkItemListsFor(
                sampleParticipantName, sampleProcessDefinitionName);

        int size = entries.size();

        wiListsBean.saveWorkItemsListFor(samplePid, sampleParticipantName, null);

        entries = wiListsBean.getWorkItemListsFor(sampleParticipantName,
                sampleProcessDefinitionName);

        assertEquals(size + 1, entries.size());

        WorkItemsListEntryImpl wile = (WorkItemsListEntryImpl) entries
                .iterator().next();
        assertEquals(2, wile.getWorkItemEntries().size());

        int formerSize = wapiBean.listWorkItems(samplePidWithNoWorkItem,
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL).size();
        assertEquals(0, formerSize);

        wiListsBean.restoreWorkItemsListFor(samplePidWithNoWorkItem,
                wile.getEntryId(), false, true);

        int newSize = wapiBean.listWorkItems(samplePidWithNoWorkItem,
                WMWorkItemState.WORKFLOW_TASK_STATE_ALL).size();

        assertEquals(2, newSize);

        wiListsBean.deleteWorkItemsListById(entries.iterator().next()
                .getEntryId());
        entries = wiListsBean.getWorkItemListsFor(sampleParticipantName,
                sampleProcessDefinitionName);

        assertEquals(size, entries.size());
    }

    private void startSampleProc() throws WMWorkflowException {
        WMProcessDefinition pdef = wapiBean
                .getProcessDefinitionByName(sampleProcessDefinitionName);
        WMActivityInstance ai = wapiBean.startProcess(pdef.getId(), null, null);
        samplePid = ai.getProcessInstance().getId();
        assertNotNull(samplePid);

        // Create 2 tasks
        Collection<WMWorkItemDefinition> defs = wapiBean
                .getWorkItemDefinitionsFor(samplePid);
        sampleProcessWorkItemDefinitionId = defs.iterator().next().getId();

        WMWorkItemInstance wi = wapiBean.createWorkItem(samplePid,
                sampleProcessWorkItemDefinitionId, null);
        wapiBean.assignWorkItem(wi.getId(), new WMParticipantImpl("foo"));

        wi = wapiBean.createWorkItem(samplePid,
                sampleProcessWorkItemDefinitionId, null);
        wapiBean.assignWorkItem(wi.getId(), new WMParticipantImpl("bar"));

        ai = wapiBean.startProcess(pdef.getId(), null, null);
        samplePidWithNoWorkItem = ai.getProcessInstance().getId();
        assertNotNull(samplePidWithNoWorkItem);
    }

    private void killSampleProc() throws WMWorkflowException {
        wapiBean.terminateProcessInstance(samplePid);
        wapiBean.terminateProcessInstance(samplePidWithNoWorkItem);
    }

}
