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
package org.nuxeo.functionaltests.pages.tabs;

/**
 * @since 5.7
 */
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import org.junit.Assert;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class WorkflowTabSubPage extends DocumentBasePage {

    @FindBy(xpath = "//form[contains(@id, 'nxl_tasks_form')]")
    public WebElement workflowTasksForm;

    @FindBy(xpath = "//select[contains(@id, 'nxw_validationOrReview')]")
    public WebElement reviewSelector;

    public WorkflowTabSubPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Add reviewer in default serial workflow
     */
    public void addWorkflowReviewer(final String username) {
        Select2WidgetElement particpants = new Select2WidgetElement(driver,
                driver.findElement(By.xpath("//div[contains(@id, 'nxw_participants_select2')]")), true);
        particpants.selectValue(username);
        selectItemInDropDownMenu(reviewSelector, "Simple Review");
    }

    /**
     * Add reviewer in default parallel workflow
     *
     * @since 5.9.1
     */
    public void addParallelWorkflowReviewer(String user) {
        Select2WidgetElement particpants = new Select2WidgetElement(driver,
                driver.findElement(By.xpath("//div[contains(@id, 'nxw_participants_select2')]")), true);
        particpants.selectValue(user);
    }

    /**
     * @since 5.9.1
     */
    public void addParallelWorkflowEndDate() {
        DateFormat sdf = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.ENGLISH);
        WebElement endDate = driver.findElement((By.xpath("//input[contains(@id, 'nxw_end_dateInputDate')]")));
        endDate.sendKeys(sdf.format(new Date()));
        // validate input date
        Assert.assertTrue(endDate.getAttribute("value").equals(sdf.format(new Date())));
    }

    public void showGraphView() {
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        findElementWaitUntilEnabledAndClick(By.linkText("Show Graph View"));
        arm.end();
        // wait for load
        AbstractPage.getFancyBoxContent();
        Locator.waitUntilElementPresent(By.id("fancybox-close"));
        // wait for graph to be loaded to avoid js errors when closing the fancybox too early
        Locator.waitUntilElementPresent(By.id("jsPlumb_1_6"));
    }

    public void closeGraphView() {
        AbstractPage.closeFancyBox();
    }

    public void startWorkflow() {
        findElementWaitUntilEnabledAndClick(By.xpath("//input[@value='Start the Review']"));
    }

    /**
     * @since 5.9.1
     */
    public void endTask(String taskName, String comment) {
        findElementAndWaitUntilEnabled(By.tagName("textarea")).sendKeys(comment);
        findElementWaitUntilEnabledAndClick(By.xpath(String.format("//input[@value='%s']", taskName)));
    }

    public void endTask(String taskName) {
        findElementWaitUntilEnabledAndClick(By.xpath(String.format("//input[@value='%s']", taskName)));
    }

    /**
     * @since 8.3
     */
    public <T> T endTask(String taskName, Class<T> pageClassToProxy) {
        endTask(taskName);
        return asPage(pageClassToProxy);
    }

    /**
     * @since 5.8
     */
    public WebElement getTaskLayoutNode() {
        return findElementWithTimeout(
                By.xpath("//div[starts-with(@id, 'nxl_current_route_layout:nxw_current_route_user_tasks_panel')]"));
    }

    @Override
    public SummaryTabSubPage getSummaryTab() {
        clickOnDocumentTabLink(summaryTabLink);
        return asPage(SummaryTabSubPage.class);
    }
}
