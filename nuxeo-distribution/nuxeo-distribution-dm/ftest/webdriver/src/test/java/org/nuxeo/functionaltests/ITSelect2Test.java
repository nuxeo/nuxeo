/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */
package org.nuxeo.functionaltests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersGroupsBasePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.AccessRightsSubPage;
import org.nuxeo.functionaltests.pages.tabs.EditTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Select2 feature test.
 *
 * @since 5.7.3
 */
public class ITSelect2Test extends AbstractTest {

    private final static String WORKSPACE_TITLE = "WorkspaceTitle_"
            + new Date().getTime();

    public final static String[] SUBJECTS = { "Comics", "Religion", "Education" };

    public final static String COVERAGE = "France";

    public static final String S2_COVERAGE_FIELD_XPATH = "//*[@id='s2id_document_edit:nxl_dublincore:nxw_coverage_select2']/a/span";

    /**
     * Delete created user and data.
     *
     * @throws UserNotConnectedException
     *
     * @since 5.7.3
     */
    private void restoreSate() throws Exception {
        UsersTabSubPage usersTab = login().getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        usersTab = usersTab.searchUser(TEST_USERNAME);
        usersTab = usersTab.viewUser(TEST_USERNAME).deleteUser();
        DocumentBasePage documentBasePage = usersTab.exitAdminCenter().getHeaderLinks().getNavigationSubPage().goToDocument(
                "Workspaces");
        deleteWorkspace(documentBasePage, WORKSPACE_TITLE);
        logout();
    }

    /**
     * Create a file document and manipulate coverage and subjects fields based
     * on select2 attributes.
     *
     * @throws Exception
     *
     * @since 5.7.3
     */
    @Test
    public void testSelect2Edit() throws Exception {
        DocumentBasePage documentBasePage;

        DocumentBasePage s = login();

        // Create a new user if not exist
        UsersGroupsBasePage page;
        UsersTabSubPage usersTab = s.getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        usersTab = usersTab.searchUser(TEST_USERNAME);
        if (!usersTab.isUserFound(TEST_USERNAME)) {
            page = usersTab.getUserCreatePage().createUser(TEST_USERNAME, TEST_USERNAME,
                    "lastname1", "company1", "email1", TEST_PASSWORD, "members");
            usersTab = page.getUsersTab(true);
        } // search user usersTab =
        usersTab.searchUser(TEST_USERNAME);
        assertTrue(usersTab.isUserFound(TEST_USERNAME));

        // create a new wokspace and grant all rights to the test user
        documentBasePage = usersTab.exitAdminCenter().getHeaderLinks().getNavigationSubPage().goToDocument(
                "Workspaces");
        DocumentBasePage workspacePage = createWorkspace(documentBasePage,
                WORKSPACE_TITLE, null);
        AccessRightsSubPage accessRightSubTab = workspacePage.getManageTab().getAccessRightsSubTab();
        // Need WriteSecurity (so in practice Manage everything) to edit a
        // Workspace
        if (!accessRightSubTab.hasPermissionForUser("Manage everything",
                TEST_USERNAME)) {
            accessRightSubTab.addPermissionForUser(TEST_USERNAME,
                    "Manage everything", true);
        }

        logout();

        // Log as test user and edit the created workdspace
        documentBasePage = login(TEST_USERNAME, TEST_PASSWORD).getContentTab().goToDocument(
                "Workspaces").getContentTab().goToDocument(WORKSPACE_TITLE);

        // Create test File
        FileDocumentBasePage filePage = createFile(workspacePage, "Test file",
                "Test File description", false, null, null, null);
        EditTabSubPage editTabSubPage = filePage.getEditTab();
        Select2WidgetElement coverageWidget = new Select2WidgetElement(
                driver,
                driver.findElement(By.xpath("//*[@id='s2id_document_edit:nxl_dublincore:nxw_coverage_select2']")),
                false);
        coverageWidget.selectValue(COVERAGE);

        Select2WidgetElement subjectsWidget = new Select2WidgetElement(
                driver,
                driver.findElement(By.xpath("//*[@id='s2id_document_edit:nxl_dublincore:nxw_subjects_select2']")),
                true);
        subjectsWidget.selectValues(SUBJECTS);

        editTabSubPage.save();

        editTabSubPage = filePage.getEditTab();

        WebElement savedCoverage = driver.findElement(By.xpath(S2_COVERAGE_FIELD_XPATH));
        assertTrue(savedCoverage.getText() != null);
        assertTrue(savedCoverage.getText().equals(COVERAGE));

        List<WebElement> savedSubjects = driver.findElements(By.xpath("//*[@id='s2id_document_edit:nxl_dublincore:nxw_subjects_select2']/ul/li/div"));
        assertEquals(savedSubjects.size(), SUBJECTS.length);

        // Remove the second subject
        WebElement deleteSecondSubjectAction = driver.findElement(By.xpath("//*[@id='s2id_document_edit:nxl_dublincore:nxw_subjects_select2']/ul/li[2]/a"));
        deleteSecondSubjectAction.click();

        // We need to do this because select2 take a little while to write in
        // the form that an entry has been deleted
        Thread.sleep(250);

        editTabSubPage.save();

        editTabSubPage = filePage.getEditTab();

        // Make sure we have one subject removed
        savedSubjects = driver.findElements(By.xpath("//*[@id='s2id_document_edit:nxl_dublincore:nxw_subjects_select2']/ul/li/div"));
        assertEquals(savedSubjects.size(), SUBJECTS.length - 1);

        logout();

        restoreSate();
    }

}
