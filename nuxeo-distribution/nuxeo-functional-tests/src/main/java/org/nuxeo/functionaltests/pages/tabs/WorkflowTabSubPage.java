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
package org.nuxeo.functionaltests.pages.tabs;

/**
 * @since 5.7
 */
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import org.junit.Assert;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class WorkflowTabSubPage extends DocumentBasePage {

    @FindBy(xpath = "//form[contains(@id, 'nxl_current_route_layout_current_route_user_tasks_form')]")
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
        Select2WidgetElement particpants = new Select2WidgetElement(
                driver,
                driver.findElement(By.xpath("//div[contains(@id, 'nxw_participants_select2')]")),
                true);
        particpants.selectValue(username);
        selectItemInDropDownMenu(reviewSelector, "Simple review");
    }

    /**
     * Add reviewer in default parallel workflow
     *
     * @since 5.9.1
     */
    public void addParallelWorkflowReviewer(String user) {
        Select2WidgetElement particpants = new Select2WidgetElement(
                driver,
                driver.findElement(By.xpath("//div[contains(@id, 'nxw_participants_select2')]")),
                true);
        particpants.selectValue(user);
    }

    /**
     * @since 5.9.1
     */
    public void addParallelWorkflowEndDate() {
        DateFormat sdf = DateFormat.getDateInstance(DateFormat.MEDIUM,
                Locale.ENGLISH);
        WebElement endDate = driver.findElement((By.xpath("//input[contains(@id, 'nxw_end_dateInputDate')]")));
        endDate.sendKeys(sdf.format(new Date()));
        // validate input date
        Assert.assertTrue(endDate.getAttribute("value").equals(
                sdf.format(new Date())));
    }

    public void showGraphView() {
        findElementAndWaitUntilEnabled(By.linkText("Show graph view")).click();
    }

    public void closeGraphView() {
        findElementAndWaitUntilEnabled(By.id("fancybox-close")).click();
    }

    public void startWorkflow() {
        findElementAndWaitUntilEnabled(
                By.xpath("//input[@value='Start the review']")).click();
    }

    /**
     * @since 5.9.1
     */
    public void endTask(String taskName, String comment) {
        findElementAndWaitUntilEnabled(By.tagName("textarea")).sendKeys(comment);
        findElementAndWaitUntilEnabled(
                By.xpath(String.format("//input[@value='%s']", taskName))).click();
    }

    public void endTask(String taskName) {
        findElementAndWaitUntilEnabled(
                By.xpath(String.format("//input[@value='%s']", taskName))).click();
    }

    /**
     * @since 5.8
     */
    public WebElement getTaskLayoutNode() {
        return findElementWithTimeout(By.xpath("//div[starts-with(@id, 'nxl_current_route_layout:nxw_current_route_user_tasks_panel')]"));
    }

    @Override
    public SummaryTabSubPage getSummaryTab() {
        clickOnLinkIfNotSelected(summaryTabLink);
        return asPage(SummaryTabSubPage.class);
    }
}
