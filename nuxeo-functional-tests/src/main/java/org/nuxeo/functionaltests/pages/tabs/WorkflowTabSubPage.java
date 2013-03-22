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
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class WorkflowTabSubPage extends DocumentBasePage {

    @FindBy(xpath = "//form[contains(@id, 'nxl_current_route_layout_current_route_user_tasks_form')]")
    public WebElement workflowTasksForm;

    @FindBy(xpath = "//input[contains(@id, 'nxw_participants_suggest')]")
    public WebElement searchTaskAssigneesInput;

    @FindBy(xpath = "//select[contains(@id, 'nxw_validationOrReview')]")
    public WebElement reviewSelector;

    public WorkflowTabSubPage(WebDriver driver) {
        super(driver);
    }

    public void addWorkflowReviewer() {
        searchTaskAssigneesInput.sendKeys("jdoe");
        findElementWaitUntilEnabledAndClick(
                By.xpath("//table[contains(@id, 'nxw_participants_suggestionBox:suggest')]/tbody/tr[1]/td[2]"),
                AbstractTest.AJAX_TIMEOUT_SECONDS * 1000,
                AbstractTest.AJAX_TIMEOUT_SECONDS * 1000);
        reviewSelector.click();
        selectItemInDropDownMenu(reviewSelector, "Simple review");
    }

    public void showGraphView() {
        findElementAndWaitUntilEnabled(By.linkText("Show graph view")).click();
    }

    public void closeGraphView() {
        findElementAndWaitUntilEnabled(By.id("fancybox-close")).click();
    }

    public void startWorkflow() {
        findElementAndWaitUntilEnabled(By.linkText("Start the review")).click();
    }

    public void endTask(String taskName) {
        findElementAndWaitUntilEnabled(By.linkText(taskName)).click();
    }

    @Override
    public SummaryTabSubPage getSummaryTab() {
        clickOnLinkIfNotSelected(summaryTabLink);
        return asPage(SummaryTabSubPage.class);
    }
}
