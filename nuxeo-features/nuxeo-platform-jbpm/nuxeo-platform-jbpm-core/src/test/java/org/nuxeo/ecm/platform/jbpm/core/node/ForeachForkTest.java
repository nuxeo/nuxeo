/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.jbpm.core.node;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jbpm.graph.def.Node;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.SimplePrincipal;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.test.AbstractProcessDefinitionTest;

/**
 * @author arussel
 *
 */
public class ForeachForkTest extends AbstractProcessDefinitionTest {

    @Override
    public String getProcessDefinitionResource() {
        return "/process/test-foreachfork.xml";
    }

    @SuppressWarnings("unchecked")
    public void testForeachNode() {
        Node node = pd.getNode("fork each participant");
        assertNotNull(node);

        jbpmContext.getGraphSession().deployProcessDefinition(pd);
        ProcessInstance pi = jbpmContext.newProcessInstance("foreach-test");
        assertNotNull(pi);

        Principal bob = new SimplePrincipal("bob");
        Principal trudy = new SimplePrincipal("trudy");
        Principal jack = new SimplePrincipal("jack");
        List<Principal> daList = new ArrayList<Principal>();
        daList.add(bob);
        daList.add(trudy);
        daList.add(jack);
        assertEquals(3, daList.size());

        pi.getContextInstance().setVariable(
                JbpmService.VariableName.participants.name(), daList);
        pi.signal();
        Set<TaskInstance> tis = (Set<TaskInstance>) pi.getTaskMgmtInstance().getTaskInstances();
        assertEquals(3, tis.size());

        for (TaskInstance ti : tis) {
            ti.end();
        }
        assertTrue(pi.hasEnded());
    }

}
