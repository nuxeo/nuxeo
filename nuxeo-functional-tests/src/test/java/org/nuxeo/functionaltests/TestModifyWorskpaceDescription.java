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
 *     Sun Seng David TAN
 *     Florent Guillaume
 */
package org.nuxeo.functionaltests;

import org.junit.Test;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.forms.WorkspaceFormPage;
import static junit.framework.Assert.assertEquals;

/**
 * <p>
 * Test Modifying a workspace description in Nuxeo DM.
 * </p>
 * <p>
 * Requirements: the user jsmith is created
 * </p>
 * <ol>
 * <li>loginAs jsmith</li>
 * <li>followLink to testWorkspace1</li>
 * <li>modifyWorkspaceDescription</li>
 * <li>logout</li>
 * </ol>
 */
public class TestModifyWorskpaceDescription extends AbstractTest {

    @Test
    public void testModifyWsDescription() {
        // login
        DocumentBasePage documentBasePage = login().getContentTab().goToDocument(
                "Workspaces");
        // create a new workspace in there named workspace1
        WorkspaceFormPage workspaceCreationFormPage = documentBasePage.getWorkspaceContentTab().getWorkspaceCreatePage();
        String workspaceTitle = "workspaceDescriptionModify";
        DocumentBasePage workspacePage = workspaceCreationFormPage.createNewWorkspace(
                workspaceTitle, "a workspace description");
        String descriptionModified = "Description modified";
        documentBasePage = workspacePage.getEditTab().edit(null,
                descriptionModified);
        assertEquals(descriptionModified,
                documentBasePage.getCurrentDocumentDescription());
        assertEquals(workspaceTitle, documentBasePage.getCurrentDocumentTitle());

    }

}
