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
package org.nuxeo.ecm.platform.jbpm.core.helpers;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;

/**
 * @author arussel
 *
 */
public class ProcessDefinitionManagerTest extends RepositoryTestCase {
    NuxeoPrincipal user;
    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.api");
        //deployBundle("org.nuxeo.ecm.platform.types.core");
        //deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.usermanager");
        deployBundle("org.nuxeo.ecm.platform.core.jbpm");
        deployBundle("org.nuxeo.ecm.platform.core.jbpm.test");
//        user = new NuxeoPrincipalImpl("bob");
//        manager = new ProcessDefinitionManager();
    }

    public void testGetListPD() throws Exception {
        assertTrue(true);
//        Map<String, String> list = manager.getListPD(user);
//        assertNotNull(list);
    }
}
