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

package org.nuxeo.functionaltests.pages.restapiDoc;

import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @since 7.3
 */
public class RestApiDocBasePage {

    @Required
    @FindBy(id = "api_info")
    public WebElement apiInfoDiv;

    @Required
    @FindBy(linkText = "path")
    public WebElement pathLink;

    @Required
    @FindBy(linkText = "id")
    public WebElement idLink;

    @Required
    @FindBy(linkText = "query")
    public WebElement queryLink;

    @Required
    @FindBy(linkText = "automation")
    public WebElement automationLink;

    @Required
    @FindBy(linkText = "user")
    public WebElement userLink;

    @Required
    @FindBy(linkText = "group")
    public WebElement groupLink;

    @Required
    @FindBy(linkText = "directory")
    public WebElement directoryLink;

    @Required
    @FindBy(linkText = "childrenAdapter")
    public WebElement childrenAdapterLink;

    @Required
    @FindBy(linkText = "searchAdapter")
    public WebElement searchAdapterLink;

    @Required
    @FindBy(linkText = "ppAdapter")
    public WebElement ppAdapterLink;

    @Required
    @FindBy(linkText = "auditAdapter")
    public WebElement auditAdapterLink;

    @Required
    @FindBy(linkText = "aclAdapter")
    public WebElement aclAdapterLink;

    @Required
    @FindBy(linkText = "boAdapter")
    public WebElement boAdapterLink;

    @Required
    @FindBy(linkText = "workflow")
    public WebElement workflowLink;

    @Required
    @FindBy(linkText = "workflowModel")
    public WebElement workflowModelLink;

    @Required
    @FindBy(linkText = "task")
    public WebElement taskLink;

}
