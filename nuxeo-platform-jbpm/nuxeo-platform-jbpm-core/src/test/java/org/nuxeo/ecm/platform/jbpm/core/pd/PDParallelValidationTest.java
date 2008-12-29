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
package org.nuxeo.ecm.platform.jbpm.core.pd;

import java.io.InputStream;

import junit.framework.TestCase;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;

/**
 * @author arussel
 *
 */
public class PDParallelValidationTest extends TestCase {
    private JbpmConfiguration configuration;

    private ProcessDefinition pd;

    @Override
    protected void setUp() throws Exception {
        InputStream is = getClass().getResourceAsStream(
                "/process/parallel-validation.xml");
        assertNotNull(is);
        pd = ProcessDefinition.parseXmlInputStream(is);
        assertNotNull(pd);
        InputStream isConf = getClass().getResourceAsStream("/config/test-jbpm.cfg.xml");
        configuration = JbpmConfiguration.parseInputStream(isConf);
        assertNotNull(configuration);
    }

    public void testPD() {
        JbpmContext context = null;
        try {
            context = configuration.createJbpmContext();
            assertNotNull(context);
            context.deployProcessDefinition(pd);
        } finally {
            context.close();
        }
    }
}
