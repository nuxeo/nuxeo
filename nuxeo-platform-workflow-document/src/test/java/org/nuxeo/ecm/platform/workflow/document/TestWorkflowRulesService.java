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
 * $Id: TestWorkflowService.java 6230 2006-11-14 09:42:31Z janguenot $
 */

package org.nuxeo.ecm.platform.workflow.document;

import java.util.Collection;

import org.nuxeo.ecm.platform.workflow.document.api.rules.WorkflowRulesManager;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * Test the workflow rules service extensions.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestWorkflowRulesService extends NXRuntimeTestCase {

    private WorkflowRulesManager workflowRules;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deploy("WorkflowRulesService.xml");
        deploy("WorkflowRulesServiceTestExtensions.xml");

        workflowRules = NXWorkflowDocument.getWorkflowRulesService();
        assertNotNull(workflowRules);
    }

    public void testDocTypeRules() {
        Collection<String> defs = workflowRules.getAllowedWorkflowDefinitionNamesByDoctype("File");
        assertEquals(2, defs.size());
        assertTrue(defs.contains("def1"));
        assertTrue(defs.contains("def2"));

        defs = workflowRules.getAllowedWorkflowDefinitionNamesByDoctype("Folder");
        assertEquals(2, defs.size());
        assertTrue(defs.contains("ddef1"));
        assertTrue(defs.contains("ddef2"));
    }

    public void testPathRules() {
        Collection<String> defs = workflowRules.getAllowedWorkflowDefinitionNamesByPath("/workspaces");
        assertEquals(1, defs.size());
        assertTrue(defs.contains("def1"));

        defs = workflowRules.getAllowedWorkflowDefinitionNamesByPath("/sections");
        assertEquals(1, defs.size());
        assertTrue(defs.contains("def2"));
    }

}
