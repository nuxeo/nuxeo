/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.functionaltests.pages.restapiDoc;

import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
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
    @FindBy(linkText = "me")
    public WebElement meLink;

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

    @Required
    @FindBy(linkText = "search")
    public WebElement searchLink;

}
