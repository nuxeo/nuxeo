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
 * $Id: TestWorkflowDocumentBeanRemote.java 28493 2008-01-04 19:51:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.document.ejb;

import junit.framework.TestCase;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.workflow.document.api.relation.WorkflowDocumentRelationException;
import org.nuxeo.ecm.platform.workflow.document.api.relation.WorkflowDocumentRelationManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Workflow document bean remote tests.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestWorkflowDocumentBeanRemote extends TestCase {

    private static final DocumentRef DOC_REF1 = new IdRef("1");

    private static final DocumentRef DOC_REF2 = new IdRef("Z");

    private static final String WORKLFOW_INSTANCE_ID = "nxworkflow_instance_1";

    private static final String WORKLFOW_INSTANCE_ID2 = "nxworkflow_instance_2";

    WorkflowDocumentRelationManager wDocBean;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        wDocBean = getWDocBean();
        assertNotNull(wDocBean);
        wDocBean.createDocumentWorkflowRef(DOC_REF1, WORKLFOW_INSTANCE_ID);
    }

    @Override
    public void tearDown() throws Exception {
        wDocBean.deleteDocumentWorkflowRef(DOC_REF1, WORKLFOW_INSTANCE_ID);
        wDocBean = null;
        super.tearDown();
    }

    private static WorkflowDocumentRelationManager getWDocBean()
            throws Exception {
        // :FIXME: we need to provide XML service descriptor in tests.
        return Framework.getService(WorkflowDocumentRelationManager.class);
    }

    public void testDocumentRefs() {
        DocumentRef[] docRefs = wDocBean.getDocumentRefsFor(WORKLFOW_INSTANCE_ID);
        assertEquals(1, docRefs.length);
        assertEquals(DOC_REF1.hashCode(), docRefs[0].hashCode());
    }

    public void testWorkflowRefs() {
        String[] wRefs = wDocBean.getWorkflowInstanceIdsFor(DOC_REF1);
        assertEquals(1, wRefs.length);
        assertEquals(WORKLFOW_INSTANCE_ID, wRefs[0]);
    }

    public void testAddWorkflowInstanceReference()
            throws WorkflowDocumentRelationException {

        // Add a workflow instance ref to doc1
        wDocBean.createDocumentWorkflowRef(DOC_REF1, WORKLFOW_INSTANCE_ID2);

        String[] wRefs = wDocBean.getWorkflowInstanceIdsFor(DOC_REF1);
        assertEquals(2, wRefs.length);
        DocumentRef[] wDefs = wDocBean.getDocumentRefsFor(WORKLFOW_INSTANCE_ID2);
        assertEquals(1, wDefs.length);

        // Add again to check if the behavior is ok
        wDocBean.createDocumentWorkflowRef(DOC_REF1, WORKLFOW_INSTANCE_ID2);
        wRefs = wDocBean.getWorkflowInstanceIdsFor(DOC_REF1);
        assertEquals(2, wRefs.length);
        wDefs = wDocBean.getDocumentRefsFor(WORKLFOW_INSTANCE_ID2);
        assertEquals(1, wDefs.length);

        // Add a workflow instance to doc2
        wRefs = wDocBean.getWorkflowInstanceIdsFor(DOC_REF2);
        assertEquals(0, wRefs.length);
        wDocBean.createDocumentWorkflowRef(DOC_REF2, WORKLFOW_INSTANCE_ID2);
        wRefs = wDocBean.getWorkflowInstanceIdsFor(DOC_REF2);
        assertEquals(1, wRefs.length);
        wDefs = wDocBean.getDocumentRefsFor(WORKLFOW_INSTANCE_ID2);
        assertEquals(2, wDefs.length);

        // Remove the reference
        wDocBean.deleteDocumentWorkflowRef(DOC_REF1, WORKLFOW_INSTANCE_ID2);
        wDocBean.deleteDocumentWorkflowRef(DOC_REF2, WORKLFOW_INSTANCE_ID2);
    }

    public void testSameInstance() throws WorkflowDocumentRelationException {

        wDocBean.createDocumentWorkflowRef(DOC_REF2, WORKLFOW_INSTANCE_ID);

        String[] wRefs = wDocBean.getWorkflowInstanceIdsFor(DOC_REF2);
        assertEquals(1, wRefs.length);

        wDocBean.createDocumentWorkflowRef(DOC_REF2, WORKLFOW_INSTANCE_ID);

        wRefs = wDocBean.getWorkflowInstanceIdsFor(DOC_REF2);
        assertEquals(1, wRefs.length);

        wDocBean.deleteDocumentWorkflowRef(DOC_REF2, WORKLFOW_INSTANCE_ID);
    }

    public void testNonExisting() throws WorkflowDocumentRelationException {

        wDocBean.createDocumentWorkflowRef(DOC_REF2, WORKLFOW_INSTANCE_ID2);

        String[] wRefs = wDocBean.getWorkflowInstanceIdsFor(DOC_REF2);
        assertEquals(1, wRefs.length);

        DocumentRef[] dRefs = wDocBean.getDocumentRefsFor(WORKLFOW_INSTANCE_ID2);
        assertEquals(1, dRefs.length);

        // Try to delete a non existing relation
        wDocBean.deleteDocumentWorkflowRef(DOC_REF2, "fake");

        wRefs = wDocBean.getWorkflowInstanceIdsFor(DOC_REF2);
        assertEquals(1, wRefs.length);

        dRefs = wDocBean.getDocumentRefsFor(WORKLFOW_INSTANCE_ID2);
        assertEquals(1, dRefs.length);

        // Try to delete a non existing relation
        wDocBean.deleteDocumentWorkflowRef(new IdRef("fake"),
                WORKLFOW_INSTANCE_ID2);

        wRefs = wDocBean.getWorkflowInstanceIdsFor(DOC_REF2);
        assertEquals(1, wRefs.length);

        dRefs = wDocBean.getDocumentRefsFor(WORKLFOW_INSTANCE_ID2);
        assertEquals(1, dRefs.length);

        // Final cleanup
        wDocBean.deleteDocumentWorkflowRef(DOC_REF2, WORKLFOW_INSTANCE_ID2);
    }

}
