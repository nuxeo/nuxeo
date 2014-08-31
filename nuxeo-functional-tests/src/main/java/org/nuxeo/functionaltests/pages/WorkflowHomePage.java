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
package org.nuxeo.functionaltests.pages;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.fragment.WebFragmentImpl;
import org.nuxeo.functionaltests.pages.tabs.SummaryTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

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
    @FindBy(id = "nxl_userOpenTasksLayout:contentViewLayoutSelectForm_cv_user_open_tasks_nxw_current_user_open_tasks:refreshContentViewLink")
    protected WebElement refreshTask;

    @Required
    @FindBy(linkText = "Workflow")
    public WebElement workflowLink;

    public boolean taskExistsOnTasksDashboard(String taskName) {
        WebElement taskNameEl = Locator.findElementWithTimeout(
                By.xpath("//span[contains(@id, 'nxw_routing_task_name')]"),
                userTasksPanel);
        return taskName.equals(taskNameEl.getText());
    }

    public void processFirstTask() {
        userTasksPanel.findElement(
                By.xpath("//input[@type='submit' and @value='Process']")).click();
    }

    public SummaryTabSubPage redirectToTask(String taskTitle) {
        driver.findElement(By.linkText(taskTitle)).click();
        return new SummaryTabSubPage(driver);
    }

    public boolean isTasksDashboardEmpty() {
        return !userTasksPanel.getText().contains("Task Name");
    }

    /**
     * @since 5.9.1
     */
    public void reassignTask(String taskDirective, String user) {
        TaskFancyBoxFragment taskBox = showTaskFancyBox("Reassign task");
        taskBox.waitForTextToBePresent(taskDirective);
        Select2WidgetElement particpants = new Select2WidgetElement(
                driver,
                driver.findElement(By.xpath("//div[contains(@id, 'nxl_workflowTaskReassignmentLayout_1:nxw_task_reassignment_actors_1_select2')]")),
                true);
        particpants.selectValue(user);
        taskBox.submit();
    }

    /**
     * @since 5.9.1
     */
    public void delegateTask(String taskDirective, String user) {
        TaskFancyBoxFragment taskBox = showTaskFancyBox("Delegate task");
        taskBox.waitForTextToBePresent(taskDirective);
        Select2WidgetElement particpants = new Select2WidgetElement(
                driver,
                driver.findElement(By.xpath("//div[contains(@id, 'nxl_workflowTaskReassignmentLayout:nxw_task_reassignment_actors_select2')]")),
                true);
        particpants.selectValue(user);
        taskBox.submit();
    }

    /**
     * @since 5.9.1
     */
    public TaskFancyBoxFragment showTaskFancyBox(String taskAction) {
        driver.findElement(
                By.xpath(String.format(
                        "//input[@type='submit' and @value='%s']", taskAction))).click();
        WebElement element = this.getFancyBoxContent();
        return getWebFragment(element,
                WorkflowHomePage.TaskFancyBoxFragment.class);
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
            cancelButton.click();
            // make sure the fancybox content is not loaded anymore
            WebDriverWait wait = new WebDriverWait(driver,
                    AbstractTest.LOAD_TIMEOUT_SECONDS);
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("fancybox-content")));
        }

        public void submit() {
            sumbitButton.click();
            // make sure the fancybox content is not loaded anymore
            WebDriverWait wait = new WebDriverWait(driver,
                    AbstractTest.LOAD_TIMEOUT_SECONDS);
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("fancybox-content")));
        }
    }
}