/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License (LGPL)
 * version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * Contributors:
 *     Sun Seng David TAN <stan@nuxeo.com>
 *     Antoine Taillefer
 */
package org.nuxeo.functionaltests.pages.forms;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author Sun Seng David TAN <stan@nuxeo.com>
 *
 */
public class WorkspaceFormPage extends AbstractPage {

    @Required
    @FindBy(id = "document_create:nxl_heading:nxw_title")
    WebElement titleTextInput;

    @Required
    @FindBy(id = "document_create:nxl_heading:nxw_description")
    WebElement descriptionTextInput;

    @Required
    @FindBy(id = "document_create:nxw_documentCreateButtons_CREATE_WORKSPACE")
    WebElement createButton;

    public WorkspaceFormPage(WebDriver driver) {
        super(driver);
    }

    public DocumentBasePage createNewWorkspace(String workspaceTitle,
            String workspaceDescription) {
        titleTextInput.sendKeys(workspaceTitle);
        descriptionTextInput.sendKeys(workspaceDescription);
        createButton.click();

        return asPage(DocumentBasePage.class);
    }

}
