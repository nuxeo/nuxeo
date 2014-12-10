/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *
 */

package org.nuxeo.functionaltests.pages.admincenter;

import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.workflow.WorkflowGraph;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 7.1
 */
public class WorkflowsPage extends AdminCenterBasePage {

    @Required
    @FindBy(id = "admin_workflow_models")
    protected WebElement adminWorkflowModelForm;

    @Required
    @FindBy(linkText = "Parallel document review")
    protected WebElement parallelDocumentReviewLink;

    @Required
    @FindBy(linkText = "Serial document review")
    protected WebElement serialDocumentReviewLink;

    public WorkflowsPage(WebDriver driver) {
        super(driver);
    }

    public WorkflowGraph getParallelDocumentReviewGraph() {
        parallelDocumentReviewLink.click();
        return asPage(WorkflowGraph.class);
    }

    public WorkflowGraph getSerialDocumentReviewGraph() {
        serialDocumentReviewLink.click();
        return asPage(WorkflowGraph.class);
    }

}