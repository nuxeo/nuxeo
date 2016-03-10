/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.functionaltests.pages.workspace;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 8.2
 */
public class WorkspaceRepositoryPage extends DocumentBasePage {

    @FindBy(id = "nxw_newDomain_form:nxw_newDomain")
    WebElement createNewDomainLink;

    @Required
    @FindBy(id = "document_content")
    WebElement documentContentForm;

    public WorkspaceRepositoryPage(WebDriver driver) {
        super(driver);
    }

    public DomainCreationFormPage getDomainCreatePage() {
        createNewDomainLink.click();
        return asPage(DomainCreationFormPage.class);
    }

}
