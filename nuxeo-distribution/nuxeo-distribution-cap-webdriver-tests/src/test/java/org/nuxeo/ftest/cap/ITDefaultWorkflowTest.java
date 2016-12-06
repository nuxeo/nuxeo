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
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.FakeSmtpMailServerFeature;
import org.nuxeo.functionaltests.AbstractTest;
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
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

/**
 * @since 5.7
 */

@RunWith(FeaturesRunner.class)
@Features({ FakeSmtpMailServerFeature.class })
public class ITDefaultWorkflowTest extends AbstractTest {

    private static final String USER_LINNET = "linnet_workflow";

    private static final String USER_BREE = "bree_workflow";

    private static final String USER_JSMITH = "jsmith_workflow";

    private static final String USER_JDOE = "jdoe_workflow";

    private static final String USER_NO_RIGHT = "no_right_workflow";

    @Override
    protected DocumentBasePage initRepository(DocumentBasePage currentPage) throws Exception {
        // Create test Workspace
        DocumentBasePage workspacePage = super.initRepository(currentPage);
        // Create test File
        FileDocumentBasePage filePage = createFile(workspacePage, "Test file", "Test File description", false, null,
                null, null);
        return filePage;
    }

    protected void createTestUser(String username, String pswd) throws Exception {
        UsersTabSubPage usersTab = login().getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        createTestUser(usersTab, username, pswd, "members");
        logout();
    }

    protected UsersTabSubPage createTestUser(UsersTabSubPage usersTab, String username, String pswd) throws Exception {
        return createTestUser(usersTab, username, pswd, "members");
    }

    /**
     * @since 8.3
     */
    protected UsersTabSubPage createTestUser(UsersTabSubPage usersTab, String username, String pswd, String group) throws Exception {
        UsersGroupsBasePage page;
        usersTab = usersTab.searchUser(username);
        if (!usersTab.isUserFound(username)) {
            page = usersTab.getUserCreatePage().createUser(username, username, "lastname1", "company1",
                    username + "@test.com", pswd, group);
            usersTab = page.getUsersTab(true);
            // make sure user has been created
            usersTab = usersTab.searchUser(username);
            assertTrue(usersTab.isUserFound(username));
        }
        return usersTab;
    }

    protected void deleteTestUser(String username) throws Exception {
        UsersTabSubPage usersTab = login().getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        deleteTestUser(usersTab, username);
        logout();
    }

    protected UsersTabSubPage deleteTestUser(UsersTabSubPage usersTab, String username) throws Exception {
        usersTab = usersTab.searchUser(username);
        usersTab = usersTab.viewUser(username).deleteUser();
        usersTab = usersTab.searchUser(username);
        assertFalse(usersTab.isUserFound(username));
        return usersTab;
    }

    @Test
    public void testDefaultSerialWorkflow() throws Exception {
        UsersTabSubPage usersTab = login().getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        usersTab = createTestUser(usersTab, USER_NO_RIGHT, USER_NO_RIGHT, null);
        // create a file doc
        DocumentBasePage defaultDomainPage = login();
        DocumentBasePage filePage = initRepository(defaultDomainPage);
        // start the default serial workflow and choose jdoe_workflow as
        // reviewer
        filePage = startDefaultSerialWorkflow(filePage, USER_NO_RIGHT);

        logout();

        UserHomePage homePage = getLoginPage().login(USER_NO_RIGHT, USER_NO_RIGHT, UserHomePage.class);
        // check that jdoe_workflow has an open task on his tasks dashboard
        WorkflowHomePage workflowHomePage = homePage.getWorkflowHomePage();
        assertTrue(workflowHomePage.taskExistsOnTasksDashboard("Validate the Document"));
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
        String participantsOnTheReview = taskLayoutDiv.findElement(By.className("user")).getText();

        assertEquals(USER_NO_RIGHT + " lastname1", participantsOnTheReview);
        homePage = workflowTab.endTask("Validate", UserHomePage.class);

        // check that USER_NO_RIGHT doesn't have the task on his workflow tasks
        // dashboard
        workflowHomePage = homePage.getWorkflowHomePage();
        assertTrue(workflowHomePage.isTasksDashboardEmpty());

        // cleanup file doc and user
        logout();
        login();
        cleanRepository(filePage);
        logout();
        deleteTestUser(USER_NO_RIGHT);
    }

    @Test
    public void testDefaultParallelWorkflow() throws Exception {
        UsersTabSubPage usersTab = login().getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        usersTab = createTestUser(usersTab, USER_JDOE, USER_JDOE);
        usersTab = createTestUser(usersTab, USER_JSMITH, USER_JSMITH);
        logout();

        // create a file doc
        DocumentBasePage defaultDomainPage = login();
        DocumentBasePage filePage = initRepository(defaultDomainPage);
        // start the default parallel workflow and choose jdoe_workflow and
        // jsmith_workflow as
        // reviewers
        filePage = startDefaultParallelWorkflow(filePage);

        logout();
        filePage = login(USER_JDOE, USER_JDOE);

        UserHomePage homePage = filePage.getUserHome();
        // check that jdoe_workflow has an open task on his tasks dashboard
        WorkflowHomePage workflowHomePage = homePage.getWorkflowHomePage();
        assertTrue(workflowHomePage.taskExistsOnTasksDashboard("Give your Opinion"));
        workflowHomePage.processFirstTask();
        SummaryTabSubPage summaryTabPage = workflowHomePage.redirectToTask("Test file");
        // check that the open task is displayed on the summary page

        assertTrue(summaryTabPage.workflowAlreadyStarted());
        assertTrue(summaryTabPage.parallelOpenTaskForCurrentUser());

        // switch to workflow tab and validate task
        WorkflowTabSubPage workflowTab = summaryTabPage.getWorkflow();

        workflowTab.endTask("Approve");

        // check that jsmith_workflow has an open task on his dashboard
        filePage = login(USER_JSMITH, USER_JSMITH);

        homePage = filePage.getUserHome();
        workflowHomePage = homePage.getWorkflowHomePage();
        assertTrue(workflowHomePage.taskExistsOnTasksDashboard("Give your Opinion"));
        workflowHomePage.processFirstTask();
        summaryTabPage = workflowHomePage.redirectToTask("Test file");
        // check that the open task is displayed on the summary page

        assertTrue(summaryTabPage.workflowAlreadyStarted());
        assertTrue(summaryTabPage.parallelOpenTaskForCurrentUser());

        // switch to workflow tab and validate task
        workflowTab = summaryTabPage.getWorkflow();

        workflowTab.endTask("Reject");

        // login with Administrator. the workflow initiator to check the final
        // task

        filePage = login();

        homePage = filePage.getUserHome();
        workflowHomePage = homePage.getWorkflowHomePage();
        assertTrue(workflowHomePage.taskExistsOnTasksDashboard("Consolidate the Review"));
        workflowHomePage.processFirstTask();
        summaryTabPage = workflowHomePage.redirectToTask("Test file");

        assertTrue(summaryTabPage.workflowAlreadyStarted());

        // switch to workflow tab and validate task
        workflowTab = summaryTabPage.getWorkflow();

        workflowTab.endTask("Approve");

        // check that the workflow was ended but jdoe_workflow doesn't have the
        // right to
        // start another workflow
        summaryTabPage = workflowTab.getSummaryTab();

        assertEquals("Approved", summaryTabPage.getCurrentLifeCycleState());

        // Check that the wf selector and the start button are no longer visible since we can't start a default workflow
        // on a document on which the current lifecycle state is 'approved'
        try {
            driver.findElement(By.xpath(SummaryTabSubPage.WORKFLOW_SELECTOR_XPATH));
            fail("Default workflow should not be started on 'Approved' documents");
        } catch (NoSuchElementException e) {
            // expected
        }
        try {
            driver.findElement(By.xpath(SummaryTabSubPage.WORKFLOW_START_BUTTON_XPATH));
            fail("Default workflow should not be started on 'Approved' documents");
        } catch (NoSuchElementException e) {
            // expected
        }

        homePage = filePage.getUserHome();
        // check that Administrator doesn't have the task on his workflow tasks
        // dashboard
        workflowHomePage = homePage.getWorkflowHomePage();
        assertTrue(workflowHomePage.isTasksDashboardEmpty());

        // cleanup file doc and user
        logout();
        login();
        cleanRepository(filePage);
        logout();

        usersTab = login().getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        usersTab = deleteTestUser(usersTab, USER_JDOE);
        usersTab = deleteTestUser(usersTab, USER_JSMITH);
        logout();
    }

    @Test
    public void testTaskReassignmentAndDelegation() throws Exception {
        UsersTabSubPage usersTab = login().getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        usersTab = createTestUser(usersTab, USER_JDOE, USER_JDOE);
        usersTab = createTestUser(usersTab, USER_BREE, USER_BREE);
        usersTab = createTestUser(usersTab, USER_JSMITH, USER_JSMITH);
        usersTab = createTestUser(usersTab, USER_LINNET, USER_LINNET);
        logout();

        // create a file doc
        DocumentBasePage defaultDomainPage = login();
        DocumentBasePage filePage = initRepository(defaultDomainPage);
        // start the default parallel workflow and choose jdoe_workflow and
        // jsmith_workflow as
        // reviewers
        filePage = startDefaultParallelWorkflow(filePage);

        logout();
        filePage = login(USER_JDOE, USER_JDOE);

        UserHomePage homePage = filePage.getUserHome();
        // check that jdoe_workflow has an open task on his tasks dashboard
        WorkflowHomePage workflowHomePage = homePage.getWorkflowHomePage();
        assertTrue(workflowHomePage.taskExistsOnTasksDashboard("Give your Opinion"));
        workflowHomePage.processFirstTask();

        // reassign task to bree_workflow
        workflowHomePage.reassignTask("Give your Opinion", USER_BREE);
        // check that jdoe_workflow has no longer the task
        assertTrue(workflowHomePage.isTasksDashboardEmpty());

        // login with bree_workflow to process this task
        filePage = login(USER_BREE, USER_BREE);

        homePage = filePage.getUserHome();
        workflowHomePage = homePage.getWorkflowHomePage();
        assertTrue(workflowHomePage.taskExistsOnTasksDashboard("Give your Opinion"));
        workflowHomePage.processFirstTask();

        SummaryTabSubPage summaryTabPage = workflowHomePage.redirectToTask("Test file");
        // check that the open task is displayed on the summary page

        assertTrue(summaryTabPage.workflowAlreadyStarted());
        assertTrue(summaryTabPage.parallelOpenTaskForCurrentUser());

        WorkflowTabSubPage workflowTab = summaryTabPage.getWorkflow();

        workflowTab.endTask("Approve", "Approve comment");

        // check that jsmith_workflow has an open task on his dashboard
        filePage = login(USER_JSMITH, USER_JSMITH);

        homePage = filePage.getUserHome();
        workflowHomePage = homePage.getWorkflowHomePage();
        assertTrue(workflowHomePage.taskExistsOnTasksDashboard("Give your Opinion"));
        workflowHomePage.processFirstTask();
        // delegate his task to linnet_workflow
        workflowHomePage.delegateTask("Give your Opinion", USER_LINNET);
        // test that jsmith_workflow can still see the task
        assertTrue(workflowHomePage.taskExistsOnTasksDashboard("Give your Opinion"));

        // login with linnet_workflow to process the task
        filePage = login(USER_LINNET, USER_LINNET);

        homePage = filePage.getUserHome();
        workflowHomePage = homePage.getWorkflowHomePage();
        assertTrue(workflowHomePage.taskExistsOnTasksDashboard("Give your Opinion"));
        workflowHomePage.processFirstTask();
        summaryTabPage = workflowHomePage.redirectToTask("Test file");
        // check that the open task is displayed on the summary page

        assertTrue(summaryTabPage.workflowAlreadyStarted());
        assertTrue(summaryTabPage.parallelOpenTaskForCurrentUser());

        // switch to workflow tab and validate task
        workflowTab = summaryTabPage.getWorkflow();

        workflowTab.endTask("Reject", "Reject comment");

        // login with Administrator. the workflow initiator to check the final
        // task

        filePage = login();

        homePage = filePage.getUserHome();
        workflowHomePage = homePage.getWorkflowHomePage();
        assertTrue(workflowHomePage.taskExistsOnTasksDashboard("Consolidate the Review"));
        workflowHomePage.processFirstTask();
        summaryTabPage = workflowHomePage.redirectToTask("Test file");

        assertTrue(summaryTabPage.workflowAlreadyStarted());

        // check the consultation wrap-up
        workflowTab = summaryTabPage.getWorkflow();
        WebElement taskLayoutDiv = workflowTab.getTaskLayoutNode();
        String parallelConsultationWrapUp = taskLayoutDiv.findElement(
                By.xpath("//span[contains(@id, 'nxw_review_result')]")).getText();

        assertTrue(parallelConsultationWrapUp.contains("bree_workflow lastname1 bree_workflow OK Approve comment"));
        assertTrue(parallelConsultationWrapUp.contains("linnet_workflow lastname1 linnet_workflow KO Reject comment"));
        // end the last task
        workflowTab.endTask("Approve");

        summaryTabPage = workflowTab.getSummaryTab();

        assertEquals("Approved", summaryTabPage.getCurrentLifeCycleState());

        homePage = filePage.getUserHome();
        // check that Administrator doesn't have the task on his workflow tasks
        // dashboard
        workflowHomePage = homePage.getWorkflowHomePage();
        assertTrue(workflowHomePage.isTasksDashboardEmpty());

        // cleanup file doc and user
        logout();
        login();
        cleanRepository(filePage);
        logout();

        usersTab = login().getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        usersTab = deleteTestUser(usersTab, USER_JDOE);
        usersTab = deleteTestUser(usersTab, USER_JSMITH);
        usersTab = deleteTestUser(usersTab, USER_BREE);
        usersTab = deleteTestUser(usersTab, USER_LINNET);
        logout();
    }

    protected DocumentBasePage startDefaultSerialWorkflow(DocumentBasePage filePage, final String username) {
        // start workflow
        SummaryTabSubPage summaryTabPage = filePage.getSummaryTab();
        summaryTabPage.startDefaultWorkflow();
        assertTrue(summaryTabPage.workflowTasksForm.getText().contains("Please select some participants for the review"));
        // click on the workflow tab
        WorkflowTabSubPage workflowTab = filePage.getWorkflow();
        workflowTab.showGraphView();
        workflowTab.closeGraphView();
        workflowTab.addWorkflowReviewer(username);
        workflowTab.startWorkflow();
        summaryTabPage = filePage.getSummaryTab();
        assertTrue(summaryTabPage.workflowAlreadyStarted());
        return filePage;
    }

    protected DocumentBasePage startDefaultParallelWorkflow(DocumentBasePage filePage) {
        // start workflow
        SummaryTabSubPage summaryTabPage = filePage.getSummaryTab();
        summaryTabPage.startDefaultParallelWorkflow();
        assertTrue(summaryTabPage.workflowTasksForm.getText().contains("Please select some participants for the review"));
        // click on the workflow tab
        WorkflowTabSubPage workflowTab = filePage.getWorkflow();
        workflowTab.showGraphView();
        workflowTab.closeGraphView();
        workflowTab.addParallelWorkflowReviewer(USER_JDOE);
        workflowTab.addParallelWorkflowReviewer(USER_JSMITH);
        workflowTab.addParallelWorkflowEndDate();
        workflowTab.startWorkflow();

        summaryTabPage = filePage.getSummaryTab();
        assertTrue(summaryTabPage.workflowAlreadyStarted());
        return filePage;
    }
}