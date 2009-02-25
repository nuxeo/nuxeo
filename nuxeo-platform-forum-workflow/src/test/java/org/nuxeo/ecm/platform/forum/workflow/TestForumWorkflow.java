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
 *     Anahide Tchertchian
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.forum.workflow;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.core.service.JbpmServiceImpl;
import org.nuxeo.ecm.platform.jbpm.test.JbpmTestConstants;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Anahide Tchertchian
 *
 */
public class TestForumWorkflow extends RepositoryOSGITestCase {

    private JbpmService service;

    private UserManager userManager;

    private NuxeoPrincipal administrator;

    private NuxeoPrincipal user1;

    @Override
    protected void setUp() throws Exception {
        // clean up previous test.
        JbpmServiceImpl.contexts.set(null);
        super.setUp();
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.platform.usermanager");
        deployBundle("org.nuxeo.ecm.directory.types.contrib");
        deployBundle("org.nuxeo.ecm.directory.sql");

        deployBundle(JbpmTestConstants.CORE_BUNDLE_NAME);
        deployBundle(JbpmTestConstants.TESTING_BUNDLE_NAME);
        deployBundle(ForumWorkflowTestConstants.FORUM_CORE_BUNDLE);
        deployBundle(ForumWorkflowTestConstants.FORUM_WORKFLOW_BUNDLE);

        service = Framework.getService(JbpmService.class);
        userManager = Framework.getService(UserManager.class);
        assertNotNull(userManager);
        administrator = userManager.getPrincipal("Administrator");
        assertNotNull(administrator);
        user1 = userManager.getPrincipal("myuser1");
        assertNotNull(user1);
        openRepository();
    }

    public void test() throws Exception {
        // TODO
    }

}
