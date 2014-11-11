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
 *     Sun Seng David TAN <stan@nuxeo.com>
 */
package org.nuxeo.functionaltests.pages.tabs;

import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author Sun Seng David TAN <stan@nuxeo.com>
 *
 */
public class SummaryTabSubPage extends AbstractPage {

    @FindBy(xpath = "//input[contains(@id, 'nxw_start_route_widget_start_route')]")
    public WebElement startWorkflowBtn;

    @FindBy(xpath = "//select[contains(@id, 'nxw_start_route_widget')]")
    public WebElement workflowSelector;

    @FindBy(xpath = "//form[contains(@id, 'nxl_grid_summary_layout_summary_current_document_single_tasks_form')]")
    public WebElement workflowTasksForm;

    public SummaryTabSubPage(WebDriver driver) {
        super(driver);
    }

    public void startDefaultWorkflow() {
        selectItemInDropDownMenu(workflowSelector, "Serial document review");
        startWorkflowBtn.click();
    }

    public boolean workflowAlreadyStarted() {
        return findElementWithTimeout(
                By.xpath("//form[contains(@id, 'nxl_grid_summary_layout_summary_document_route_form')]")).getText().contains(
                "Serial document review has been started by");
    }

    public boolean openTaskForCurrentUser() {
        return findElementWithTimeout(
                By.xpath("//form[contains(@id, 'nxl_grid_summary_layout_summary_current_document_single_tasks_form')]")).getText().contains(
                "Please accept or reject the document");
    }

    public WorkflowTabSubPage getWorkflow() {
        findElementWithTimeout(By.linkText("Workflow")).click();
        return asPage(WorkflowTabSubPage.class);
    }

    public boolean cantStartWorkflow() {
        return findElementWithTimeout(
                By.xpath("//form[contains(@id, 'nxl_grid_summary_layout_summary_document_route_form')]")).getText().contains(
                "No workflow process can be started on this document.");
    }
}
