/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Mariana Cedica
 */
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ftest.cap.Constants.FILE_TYPE;
import static org.nuxeo.ftest.cap.Constants.TEST_FILE_TITLE;
import static org.nuxeo.ftest.cap.Constants.TEST_FILE_URL;
import static org.nuxeo.ftest.cap.Constants.TEST_WORKSPACE_PATH;
import static org.nuxeo.ftest.cap.Constants.TEST_WORKSPACE_TITLE;
import static org.nuxeo.ftest.cap.Constants.WORKSPACES_PATH;
import static org.nuxeo.ftest.cap.Constants.WORKSPACE_TYPE;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.FakeSmtpMailServerFeature;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.UserHomePage;
import org.nuxeo.functionaltests.pages.WorkflowHomePage;
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

    @Before
    public void before() {
        RestHelper.createUser(USER_LINNET, USER_LINNET, USER_LINNET, "lastname1", "company1", "email1", "members");
        RestHelper.createUser(USER_BREE, USER_BREE, USER_BREE, "lastname1", "company1", "email1", "members");
        RestHelper.createUser(USER_JSMITH, USER_JSMITH, USER_JSMITH, "lastname1", "company1", "email1", "members");
        RestHelper.createUser(USER_JDOE, USER_JDOE, USER_JDOE, "lastname1", "company1", "email1", "members");
        RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, TEST_WORKSPACE_TITLE, null);
        RestHelper.createDocument(TEST_WORKSPACE_PATH, FILE_TYPE, TEST_FILE_TITLE, null);
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    @Test
    public void testDefaultSerialWorkflow() throws Exception {
        // start the default serial workflow and choose jdoe_workflow as
        // reviewer
        login();
        open(TEST_FILE_URL);
        startDefaultSerialWorkflow(asPage(DocumentBasePage.class), USER_JDOE);
        logout();

        DocumentBasePage filePage = login(USER_JDOE, USER_JDOE);

        UserHomePage homePage = filePage.getUserHome();
        // check that jdoe_workflow has an open task on his tasks dashboard
        WorkflowHomePage workflowHomePage = homePage.getWorkflowHomePage();
        assertTrue(workflowHomePage.taskExistsOnTasksDashboard("Validate the Document"));
        workflowHomePage.processFirstTask();
        SummaryTabSubPage summaryTabPage = workflowHomePage.redirectToTask(TEST_FILE_TITLE);
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

        assertEquals("jdoe_workflow lastname1", participantsOnTheReview);
        workflowTab.endTask("Validate");

        summaryTabPage = workflowTab.getSummaryTab();
        homePage = filePage.getUserHome();
        // check that jdoe_workflow doesn't have the task on his workflow tasks
        // dashboard
        workflowHomePage = homePage.getWorkflowHomePage();
        assertTrue(workflowHomePage.isTasksDashboardEmpty());

        logout();
    }

    @Test
    public void testDefaultParallelWorkflow() throws Exception {
        login();
        open(TEST_FILE_URL);
        startDefaultParallelWorkflow(asPage(DocumentBasePage.class));
        logout();

        DocumentBasePage filePage = login(USER_JDOE, USER_JDOE);

        UserHomePage homePage = filePage.getUserHome();
        // check that jdoe_workflow has an open task on his tasks dashboard
        WorkflowHomePage workflowHomePage = homePage.getWorkflowHomePage();
        assertTrue(workflowHomePage.taskExistsOnTasksDashboard("Give your Opinion"));
        workflowHomePage.processFirstTask();
        SummaryTabSubPage summaryTabPage = workflowHomePage.redirectToTask(TEST_FILE_TITLE);
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
        summaryTabPage = workflowHomePage.redirectToTask(TEST_FILE_TITLE);
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
        summaryTabPage = workflowHomePage.redirectToTask(TEST_FILE_TITLE);

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

        logout();
    }

    @Test
    public void testTaskReassignmentAndDelegation() throws Exception {
        login();
        open(TEST_FILE_URL);
        startDefaultParallelWorkflow(asPage(DocumentBasePage.class));
        logout();

        DocumentBasePage filePage = login(USER_JDOE, USER_JDOE);

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

        SummaryTabSubPage summaryTabPage = workflowHomePage.redirectToTask(TEST_FILE_TITLE);
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
        summaryTabPage = workflowHomePage.redirectToTask(TEST_FILE_TITLE);
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
        summaryTabPage = workflowHomePage.redirectToTask(TEST_FILE_TITLE);

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

        logout();
    }

    protected DocumentBasePage startDefaultSerialWorkflow(DocumentBasePage filePage, final String username) {
        // start workflow
        SummaryTabSubPage summaryTabPage = filePage.getSummaryTab();
        summaryTabPage.startDefaultWorkflow();
        assertTrue(
                summaryTabPage.workflowTasksForm.getText().contains("Please select some participants for the review"));
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
        assertTrue(
                summaryTabPage.workflowTasksForm.getText().contains("Please select some participants for the review"));
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
