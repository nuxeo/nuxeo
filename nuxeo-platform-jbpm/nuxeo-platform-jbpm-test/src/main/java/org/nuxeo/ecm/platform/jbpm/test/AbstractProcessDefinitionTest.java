/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.jbpm.test;

import java.io.InputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.nuxeo.ecm.core.api.SimplePrincipal;

public abstract class AbstractProcessDefinitionTest extends TestCase {

    protected final List<String> bob_list = Collections.singletonList("bob");
    protected final List<String> trudy_list = Collections.singletonList("trudy");

    protected JbpmConfiguration configuration;

    protected ProcessDefinition pd;

    protected JbpmContext jbpmContext;

    public AbstractProcessDefinitionTest() {
    }

    public AbstractProcessDefinitionTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        InputStream isConf = getClass().getResourceAsStream(
                getConfigurationResource());
        configuration = JbpmConfiguration.parseInputStream(isConf);
        assertNotNull(configuration);

        jbpmContext = configuration.createJbpmContext();
        InputStream is = getClass().getResourceAsStream(
                getProcessDefinitionResource());
        assertNotNull(is);

        pd = ProcessDefinition.parseXmlInputStream(is);
        assertNotNull(pd);
    }

    @Override
    protected void tearDown() throws Exception {
        jbpmContext.close();
    }

    public String getConfigurationResource() {
        return "/config/test-jbpm.cfg.xml";
    }

    public abstract String getProcessDefinitionResource();

    public List<Principal> getPrincipalsList() {
        List<Principal> pList = new ArrayList<Principal>();
        pList.add(new SimplePrincipal("bob"));
        pList.add(new SimplePrincipal("trudy"));
        return pList;
    }

}
