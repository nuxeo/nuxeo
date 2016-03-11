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

import static org.nuxeo.functionaltests.Constants.SECTIONS_TITLE;
import static org.nuxeo.functionaltests.Constants.TEMPLATES_TITLE;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_TITLE;

import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.tabs.SectionsContentTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * @since 8.2
 */
public class WorkspaceHomePage extends DocumentBasePage {

    public WorkspaceHomePage(WebDriver driver) {
        super(driver);
    }

    public WorkspaceRepositoryPage goToRepository() {
        WebElement first = breadcrumbForm.findElements(By.className("jsBreadcrumbActionList"))
                                         .get(0)
                                         .findElement(By.tagName("li"));
        first.click();
        first.findElement(By.linkText("Repository")).click();
        return asPage(WorkspaceRepositoryPage.class);
    }

    public SectionsContentTabSubPage goToDocumentSections() {
        return getContentTab().goToDocument(SECTIONS_TITLE).asPage(SectionsContentTabSubPage.class);
    }

    public DocumentBasePage goToDocumentTemplates() {
        return getContentTab().goToDocument(TEMPLATES_TITLE);
    }

    public DocumentBasePage goToDocumentWorkspaces() {
        return getContentTab().goToDocument(WORKSPACES_TITLE);
    }

}
