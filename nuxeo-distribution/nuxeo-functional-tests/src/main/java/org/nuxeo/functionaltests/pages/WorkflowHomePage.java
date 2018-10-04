/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Yannis JULIENNE
 */
package org.nuxeo.functionaltests.pages;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.fragment.WebFragmentImpl;
import org.nuxeo.functionaltests.pages.tabs.SummaryTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 5.8
 */
public class WorkflowHomePage extends AbstractPage {

    protected static final Log log = LogFactory.getLog(WorkflowHomePage.class);

    public WorkflowHomePage(WebDriver driver) {
        super(driver);
    }

    @Required
    @FindBy(xpath = "//div[contains(@id, 'cv_user_open_tasks_nxw_current_user_open_tasks_resultsPanel')]")
    protected WebElement userTasksPanel;

    @Required
    @FindBy(id = "nxl_userOpenTasksLayout:nxw_contentViewActions_refreshContentView_form:nxw_contentViewActions_refreshContentView")
    protected WebElement refreshTask;

    @Required
    @FindBy(linkText = "Workflow")
    public WebElement workflowLink;

    public boolean taskExistsOnTasksDashboard(String taskName) {
        WebElement taskNameEl = Locator.findElementWithTimeout(
                By.xpath("//span[contains(@id, 'nxw_routing_task_name')]"), userTasksPanel);
        return taskName.equals(taskNameEl.getText());
    }

    public boolean taskDisplaysDocumentOnTasksDashboard(String docTitle) {
        WebElement targetDocumentTd = Locator.findElementWithTimeout(
                By.xpath("//td[contains(text(), '" + docTitle + "')]"), userTasksPanel);
        return targetDocumentTd != null;
    }

    public void processFirstTask() {
        WebElement processButton = userTasksPanel.findElement(By.xpath("//input[@type='submit' and @value='Process']"));
        waitUntilEnabledAndClick(processButton);
    }

    public SummaryTabSubPage redirectToTask(String taskTitle) {
        findElementWaitUntilEnabledAndClick(By.linkText(taskTitle));
        return new SummaryTabSubPage(driver);
    }

    public boolean isTasksDashboardEmpty() {
        return !userTasksPanel.getText().contains("Task Name");
    }

    /**
     * @since 5.9.1
     */
    public void reassignTask(String taskDirective, String user) {
        TaskFancyBoxFragment taskBox = showTaskFancyBox("Reassign Task");
        taskBox.waitForTextToBePresent(taskDirective);
        Select2WidgetElement particpants = new Select2WidgetElement(driver,
                driver.findElement(By
                                     .xpath("//div[contains(@id, 'nxl_workflowTaskReassignmentLayout_1:nxw_task_reassignment_actors_1_select2')]")),
                true);
        particpants.selectValue(user);
        taskBox.submit();
    }

    /**
     * @since 5.9.1
     */
    public void delegateTask(String taskDirective, String user) {
        TaskFancyBoxFragment taskBox = showTaskFancyBox("Delegate Task");
        taskBox.waitForTextToBePresent(taskDirective);
        Select2WidgetElement particpants = new Select2WidgetElement(driver,
                driver.findElement(By
                                     .xpath("//div[contains(@id, 'nxl_workflowTaskReassignmentLayout:nxw_task_reassignment_actors_select2')]")),
                true);
        particpants.selectValue(user);
        taskBox.submit();
    }

    /**
     * @since 5.9.1
     */
    public TaskFancyBoxFragment showTaskFancyBox(String taskAction) {
        findElementWaitUntilEnabledAndClick(
                By.xpath(String.format("//input[@type='submit' and @value='%s']", taskAction)));
        WebElement element = AbstractPage.getFancyBoxContent();
        return getWebFragment(element, WorkflowHomePage.TaskFancyBoxFragment.class);
    }

    /**
     * @since 5.9.1
     */
    public static class TaskFancyBoxFragment extends WebFragmentImpl {

        @FindBy(xpath = "//div[@id='fancybox-content']//input[@value='Cancel']")
        public WebElement cancelButton;

        @FindBy(xpath = "//div[@id='fancybox-content']//input[@type='submit']")
        public WebElement sumbitButton;

        public TaskFancyBoxFragment(WebDriver driver, WebElement element) {
            super(driver, element);
        }

        public void cancel() {
            Locator.waitUntilEnabledAndClick(cancelButton);
            AbstractPage.waitForFancyBoxClosed();
        }

        @Override
        public void submit() {
            Locator.waitUntilEnabledAndClick(sumbitButton);
            AbstractPage.waitForFancyBoxClosed();
        }

    }
}
