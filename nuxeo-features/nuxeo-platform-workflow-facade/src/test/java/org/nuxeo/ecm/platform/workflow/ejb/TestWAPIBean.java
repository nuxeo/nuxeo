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
 * $Id$
 */

package org.nuxeo.ecm.platform.workflow.ejb;

import java.util.Collection;

import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;


/**
 * Test the EJB API.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestWAPIBean extends AbstractJbpmRuntimeTestCase {

    @Override
    protected void _initializeTest() throws Exception {
        wapi = new WAPIBean();
        NB_DEPLOYED_DEFS = 3;
    }

    @Override
    public void testGetDefinitions() throws WMWorkflowException {
        Collection<WMProcessDefinition> definitions = wapi.listProcessDefinitions();
        assertEquals(3, definitions.size());
    }

    @Override
    protected void _unInitializeTest() throws Exception {
        wapi = null;
    }

}
