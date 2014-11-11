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

package org.nuxeo.ecm.platform.userworkspace.core.tests;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;

public class TestUserWorkspace extends SQLRepositoryTestCase {

    public TestUserWorkspace() {
        super("");
    }


    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.platform.content.template");
        deployBundle("org.nuxeo.ecm.platform.userworkspace.api");
        deployBundle("org.nuxeo.ecm.platform.dublincore");
        deployBundle("org.nuxeo.ecm.platform.userworkspace.types");
        deployContrib("org.nuxeo.ecm.platform.userworkspace.core", "OSGI-INF/userworkspace-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.userworkspace.core", "OSGI-INF/userWorkspaceImpl.xml");
    }

    public void testRestreictedAccess() throws Exception {

        CoreSession session = openSessionAs("toto");

        UserWorkspaceService uwm = Framework.getLocalService(UserWorkspaceService.class);
        assertNotNull(uwm);

        DocumentModel uw = uwm.getCurrentUserPersonalWorkspace(session, null);
        assertNotNull(uw);

        // check creator
        String creator = (String) uw.getProperty("dublincore", "creator");
        assertEquals("system", creator);

        // check write access
        uw.setProperty("dublibore", "description", "Toto's workspace");
        session.saveDocument(uw);
        session.save();
    }


}
