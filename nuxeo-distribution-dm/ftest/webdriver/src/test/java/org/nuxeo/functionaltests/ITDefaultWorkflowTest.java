/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Mariana Cedica
 */
package org.nuxeo.functionaltests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.FakeSmtpMailServerFeature;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.UserHomePage;
import org.nuxeo.functionaltests.pages.WorkflowHomePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersGroupsBasePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.SummaryTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.WorkflowTabSubPage;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * @since 5.7
 */

@RunWith(FeaturesRunner.class)
@Features({ FakeSmtpMailServerFeature.class })
public class ITDefaultWorkflowTest extends AbstractTest {

    @Override
    protected DocumentBasePage initRepository(DocumentBasePage currentPage)
            throws Exception {
        // Create test Workspace
        DocumentBasePage workspacePage = super.initRepository(currentPage);
        // Create test File
        FileDocumentBasePage filePage = createFile(workspacePage, "Test file",
                "Test File description", false, null, null, null);
        return filePage;
    }

    protected void createTestUser(String username, String pswd)
            throws Exception {
        UsersGroupsBasePage page;
        UsersTabSubPage usersTab = login().getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        usersTab = usersTab.searchUser(username);
        if (!usersTab.isUserFound(username)) {
            page = usersTab.getUserCreatePage().createUser(username, username,
                    "lastname1", "company1", "email1", pswd, "members");
            usersTab = page.getUsersTab(true);
        }
        // search user
        usersTab = usersTab.searchUser(username);
        assertTrue(usersTab.isUserFound(username));
        logout();
    }

    protected void deleteTestUser(String username) throws Exception {
        UsersGroupsBasePage page = login().getAdminCenter().getUsersGroupsHomePage();
        UsersTabSubPage usersTab = page.getUsersTab();
        usersTab = usersTab.searchUser(username);
        usersTab = usersTab.viewUser(username).deleteUser();
        usersTab = usersTab.searchUser(username);
        assertFalse(usersTab.isUserFound(username));
        logout();
    }

    @Test
    public void testDefaultWorkflow() throws Exception {
        createTestUser("jdoe", "test");

        // create a file doc
        DocumentBasePage defaultDomainPage = login();
        DocumentBasePage filePage = initRepository(defaultDomainPage);
        // start the default serial workflow and choose jdoe as reviewer
        filePage = startDefaultWorkflow(filePage);

        logout();
        filePage = login("jdoe", "test");

        // check that jdoe has an open task on his dashboard
        UserHomePage homePage = filePage.getUserHome();
        // check that jdoe has an open task on his tasks dashboard
        WorkflowHomePage workflowHomePage = homePage.getWorkflowHomePage();
        assertTrue(workflowHomePage.taskExistsOnTasksDashboard("Validate the document"));
        workflowHomePage.processFirstTask();
        SummaryTabSubPage summaryTabPage = workflowHomePage.redirectToTask("Test file");
        // check that the open task is displayed on the summary page

        assertTrue(summaryTabPage.workflowAlreadyStarted());
        assertTrue(summaryTabPage.openTaskForCurrentUser());

        // switch to workflow tab and validate task
        WorkflowTabSubPage workflowTab = summaryTabPage.getWorkflow();

        WebElement taskLayoutDiv = workflowTab.getTaskLayoutNode();
        // get value for Participants on the review that were set on the
        // previous step
        // the value is stored in a global workflow variable
        String participantsOnTheReview = taskLayoutDiv.findElement(
                By.className("user")).getText();

        assertEquals("jdoe lastname1", participantsOnTheReview);
        workflowTab.endTask("Validate");

        // check that the workflow was ended but jdoe doesn't have the right to
        // start another workflow
        summaryTabPage = workflowTab.getSummaryTab();
        assertTrue(summaryTabPage.cantStartWorkflow());

        // check that jdoe doesn't have any tasks on his dashboard
        homePage = filePage.getUserHome();

        assertTrue(homePage.isTaskGadgetLoaded());
        assertTrue(homePage.isTaskGadgetEmpty());

        // check that jdoe doesn't have the task on his workflow tasks dashboard
        workflowHomePage = homePage.getWorkflowHomePage();
        assertTrue(workflowHomePage.isTasksDashboardEmpty());

        // cleanup file doc and user
        logout();
        login();
        cleanRepository(filePage);
        logout();
        deleteTestUser("jdoe");
    }

    protected DocumentBasePage startDefaultWorkflow(DocumentBasePage filePage) {
        // start workflow
        SummaryTabSubPage summaryTabPage = filePage.getSummaryTab();
        summaryTabPage.startDefaultWorkflow();
        assertTrue(summaryTabPage.workflowTasksForm.getText().contains(
                "Please select some participants for the review"));
        // click on the workflow tab
        WorkflowTabSubPage workflowTab = filePage.getWorkflow();
        workflowTab.showGraphView();
        workflowTab.closeGraphView();
        workflowTab.addWorkflowReviewer();
        workflowTab.startWorkflow();
        return filePage;
    }
}