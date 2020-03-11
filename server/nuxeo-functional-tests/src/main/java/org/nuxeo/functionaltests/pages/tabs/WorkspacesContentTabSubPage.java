/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Sun Seng David TAN <stan@nuxeo.com>
 *     Antoine Taillefer
 */
package org.nuxeo.functionaltests.pages.tabs;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.forms.WorkspaceCreationFormPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author Sun Seng David TAN <stan@nuxeo.com>
 */
public class WorkspacesContentTabSubPage extends AbstractContentTabSubPage {

    @Required
    @FindBy(id = "cv_document_content_0_panel")
    WebElement contentView;

    @Required
    @FindBy(id = "nxw_newWorkspace_form:nxw_newWorkspace")
    WebElement createNewWorkspaceLink;

    public WorkspacesContentTabSubPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected WebElement getContentViewElement() {
        return contentView;
    }

    public WorkspaceCreationFormPage getWorkspaceCreatePage() {
        createNewWorkspaceLink.click();
        return asPage(WorkspaceCreationFormPage.class);
    }

}
