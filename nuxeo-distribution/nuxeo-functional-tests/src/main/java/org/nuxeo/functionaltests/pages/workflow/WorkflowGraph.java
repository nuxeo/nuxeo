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

package org.nuxeo.functionaltests.pages.workflow;

import java.util.List;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 7.1
 */
public class WorkflowGraph extends AbstractPage {

    @Required
    @FindBy(id = "nxw_TAB_DOCUMENT_ROUTE_ELEMENTS_tab_content_graphView_box")
    protected WebElement element;

    @Required
    @FindBy(name = "graphInitDone")
    protected WebElement graphInitDone;

    public WorkflowGraph(WebDriver driver) {
        super(driver);
    }

    protected static final String END_NODE_CSS_CLASS = "end_node";

    protected static final String START_NODE_CSS_CLASS = "start_node";

    public List<WebElement> getWorkflowEndNodes() {
        return element.findElements(By.cssSelector("." + END_NODE_CSS_CLASS));
    }

    public List<WebElement> getWorkflowStartNodes() {
        return element.findElements(By.cssSelector("." + START_NODE_CSS_CLASS));
    }

}
