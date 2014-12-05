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

import org.nuxeo.functionaltests.AbstractTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * @since 7.1
 */
public class WorkflowGraph {

    protected String id;

    protected WebElement element;

    protected static final String END_NODE_CSS_CLASS = "end_node";

    protected static final String START_NODE_CSS_CLASS = "start_node";

    public WorkflowGraph(final String id) {
        this.id = id;
        this.element = AbstractTest.driver.findElement(By.id(id));
    }

    public WorkflowGraph(final WebElement element) {
        this.element = element;
        this.id = element.getAttribute("id");
    }

    public List<WebElement> getWorkflowEndNodes() {
        return element.findElements(By.cssSelector("." + END_NODE_CSS_CLASS));
    }

    public List<WebElement> getWorkflowStartNodes() {
        return element.findElements(By.cssSelector("." + START_NODE_CSS_CLASS));
    }

}
