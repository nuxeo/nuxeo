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
 * $Id: AbstractWorkflowRulesServiceTestCase.java 19119 2007-05-22 11:39:21Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.document;

import org.nuxeo.ecm.platform.workflow.document.api.rules.WorkflowRulesManager;
/*
 * (C) Copyright 2006 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: AbstractWorkflowRulesServiceTestCase.java 19119 2007-05-22 11:39:21Z sfermigier $
 */
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public abstract class AbstractWorkflowRulesServiceTestCase extends
        NXRuntimeTestCase {

    protected WorkflowRulesManager workflowRules;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deploy("WorkflowRulesService.xml");
    }

    public void testRulesByDocTypes() {

        String docType = "File";

        assertEquals(0, workflowRules
                .getAllowedWorkflowDefinitionNamesByDoctype(docType).size());

        workflowRules.addRuleByType("def1", docType);
        assertEquals(1, workflowRules
                .getAllowedWorkflowDefinitionNamesByDoctype(docType).size());

        workflowRules.addRuleByType("def1", docType);
        assertEquals(1, workflowRules
                .getAllowedWorkflowDefinitionNamesByDoctype(docType).size());

        workflowRules.addRuleByType("def2", docType);
        assertEquals(2, workflowRules
                .getAllowedWorkflowDefinitionNamesByDoctype(docType).size());
        assertTrue(workflowRules.getAllowedWorkflowDefinitionNamesByDoctype(
                docType).contains("def1"));
        assertTrue(workflowRules.getAllowedWorkflowDefinitionNamesByDoctype(
                docType).contains("def2"));

        // no effect
        workflowRules.delRuleByType("fake", "fake");

        assertEquals(2, workflowRules
                .getAllowedWorkflowDefinitionNamesByDoctype(docType).size());
        assertTrue(workflowRules.getAllowedWorkflowDefinitionNamesByDoctype(
                docType).contains("def1"));
        assertTrue(workflowRules.getAllowedWorkflowDefinitionNamesByDoctype(
                docType).contains("def2"));

        // no effect
        workflowRules.delRuleByType("def1", "fake");

        assertEquals(2, workflowRules
                .getAllowedWorkflowDefinitionNamesByDoctype(docType).size());
        assertTrue(workflowRules.getAllowedWorkflowDefinitionNamesByDoctype(
                docType).contains("def1"));
        assertTrue(workflowRules.getAllowedWorkflowDefinitionNamesByDoctype(
                docType).contains("def2"));

        // no effect
        workflowRules.delRuleByType("fake", docType);

        assertEquals(2, workflowRules
                .getAllowedWorkflowDefinitionNamesByDoctype(docType).size());
        assertTrue(workflowRules.getAllowedWorkflowDefinitionNamesByDoctype(
                docType).contains("def1"));
        assertTrue(workflowRules.getAllowedWorkflowDefinitionNamesByDoctype(
                docType).contains("def2"));

        workflowRules.delRuleByType("def2", docType);

        assertEquals(1, workflowRules
                .getAllowedWorkflowDefinitionNamesByDoctype(docType).size());
        assertTrue(workflowRules.getAllowedWorkflowDefinitionNamesByDoctype(
                docType).contains("def1"));

        workflowRules.delRuleByType("def1", docType);

        assertEquals(0, workflowRules
                .getAllowedWorkflowDefinitionNamesByDoctype(docType).size());

        // Ensure no errors
        workflowRules.delRuleByType("def1", docType);

        assertEquals(0, workflowRules
                .getAllowedWorkflowDefinitionNamesByDoctype(docType).size());
    }

    public void testSimpleRulesByPath() {

        String path = "/";

        assertEquals(0, workflowRules.getAllowedWorkflowDefinitionNamesByPath(
                path).size());

        workflowRules.addRuleByPath("def1", path);
        assertEquals(1, workflowRules.getAllowedWorkflowDefinitionNamesByPath(
                path).size());

        workflowRules.addRuleByPath("def1", path);
        assertEquals(1, workflowRules.getAllowedWorkflowDefinitionNamesByPath(
                path).size());

        workflowRules.addRuleByPath("def2", path);
        assertEquals(2, workflowRules.getAllowedWorkflowDefinitionNamesByPath(
                path).size());
        assertTrue(workflowRules.getAllowedWorkflowDefinitionNamesByPath(path)
                .contains("def1"));
        assertTrue(workflowRules.getAllowedWorkflowDefinitionNamesByPath(path)
                .contains("def2"));

        // no effect
        workflowRules.delRuleByPath("fake", "fake");

        assertEquals(2, workflowRules.getAllowedWorkflowDefinitionNamesByPath(
                path).size());
        assertTrue(workflowRules.getAllowedWorkflowDefinitionNamesByPath(path)
                .contains("def1"));
        assertTrue(workflowRules.getAllowedWorkflowDefinitionNamesByPath(path)
                .contains("def2"));

        // no effect
        workflowRules.delRuleByPath("def1", "fake");

        assertEquals(2, workflowRules.getAllowedWorkflowDefinitionNamesByPath(
                path).size());
        assertTrue(workflowRules.getAllowedWorkflowDefinitionNamesByPath(path)
                .contains("def1"));
        assertTrue(workflowRules.getAllowedWorkflowDefinitionNamesByPath(path)
                .contains("def2"));

        // no effect
        workflowRules.delRuleByPath("fake", path);

        assertEquals(2, workflowRules.getAllowedWorkflowDefinitionNamesByPath(
                path).size());
        assertTrue(workflowRules.getAllowedWorkflowDefinitionNamesByPath(path)
                .contains("def1"));
        assertTrue(workflowRules.getAllowedWorkflowDefinitionNamesByPath(path)
                .contains("def2"));

        workflowRules.delRuleByPath("def2", path);

        assertEquals(1, workflowRules.getAllowedWorkflowDefinitionNamesByPath(
                path).size());
        assertTrue(workflowRules.getAllowedWorkflowDefinitionNamesByPath(path)
                .contains("def1"));

        workflowRules.delRuleByPath("def1", path);

        assertEquals(0, workflowRules.getAllowedWorkflowDefinitionNamesByPath(
                path).size());

        // Ensure no errors
        workflowRules.delRuleByPath("def1", path);

        assertEquals(0, workflowRules.getAllowedWorkflowDefinitionNamesByPath(
                path).size());
    }

    public void testRulesByPath() {

        String path1 = "/workspaces";
        String path2 = "/sections";

        assertEquals(0, workflowRules.getAllowedWorkflowDefinitionNamesByPath(
                path1).size());
        assertEquals(0, workflowRules.getAllowedWorkflowDefinitionNamesByPath(
                path2).size());

        workflowRules.addRuleByPath("def1", path1);
        assertEquals(1, workflowRules.getAllowedWorkflowDefinitionNamesByPath(
                path1).size());
        workflowRules.addRuleByPath("def2", path2);
        assertEquals(1, workflowRules.getAllowedWorkflowDefinitionNamesByPath(
                path1).size());

        assertEquals(0, workflowRules.getAllowedWorkflowDefinitionNamesByPath(
                "/").size());

        assertEquals(1, workflowRules.getAllowedWorkflowDefinitionNamesByPath(
                "/workspaces").size());

        // test case
        assertEquals(1, workflowRules.getAllowedWorkflowDefinitionNamesByPath(
                "/WorkSPaces").size());
    }

}
