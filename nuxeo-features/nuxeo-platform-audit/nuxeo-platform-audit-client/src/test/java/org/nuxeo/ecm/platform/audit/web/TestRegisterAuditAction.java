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

package org.nuxeo.ecm.platform.audit.web;

import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionService;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class TestRegisterAuditAction extends NXRuntimeTestCase {

    ActionService as;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.audit.web.tests",
                "actions-bundle.xml");
        deployContrib("org.nuxeo.ecm.platform.audit.web.tests",
                "nxauditclient-bundle.xml");
        as = (ActionService) runtime.getComponent(ActionService.ID);
    }

    public void testRegistration() {
        Action act1 = as.getActionRegistry().getAction("TAB_CONTENT_HISTORY");

        assertEquals("action.view.history", act1.getLabel());
        assertEquals("/icons/file.gif", act1.getIcon());
        assertEquals("/incl/tabs/document_history.xhtml", act1.getLink());
        assertTrue(act1.isEnabled());
    }

}
