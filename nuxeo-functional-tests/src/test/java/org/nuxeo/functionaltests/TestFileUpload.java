/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN <stan@nuxeo.com>
 */
package org.nuxeo.functionaltests;

import org.junit.Test;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.LoginPage;

/**
 * Testing file upload in nuxeo dm
 *
 * @author Sun Seng David TAN <stan@nuxeo.com>
 *
 */
public class TestFileUpload extends AbstractTest {

    @Test
    public void testLoginPage() {
        LoginPage loginPage = get("http://localhost:8080/nuxeo", LoginPage.class);
        DocumentBasePage documentBasePage = loginPage.login("Administrator",
                "Administrator", DocumentBasePage.class);
        // Get the the content tab and go to the existing Workspace root folder
        documentBasePage = documentBasePage.getContentTab().goToDocument(
                "Workspaces");


    }

}
