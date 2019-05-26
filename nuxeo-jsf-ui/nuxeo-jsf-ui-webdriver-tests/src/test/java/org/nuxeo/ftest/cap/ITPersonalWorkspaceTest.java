/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.tabs.ContentTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Non regression tests related to personal workspace management.
 *
 * @since 7.3
 */
public class ITPersonalWorkspaceTest extends AbstractTest {

    // first use case of NXP-9813
    @Test
    public void testNavigationFromSelection() throws Exception {
        DocumentBasePage page = login();
        checkMainTabs();
        ContentTabSubPage cpage = page.getContentTab();
        String docTitle = "Workspaces";
        cpage.addToWorkList(docTitle);
        WebElement clipboard = getClipboard();
        clipboard.findElement(By.linkText(docTitle)).click();
        // reload page
        page = asPage(DocumentBasePage.class);
        checkMainTabs();
        page = page.switchToPersonalWorkspace();
        checkMainTabs();
        // check tree shows personal workspace
        getTreeExplorer().findElement(By.linkText("Administrator"));
        getClipboard().findElement(By.linkText(docTitle)).click();
        // check that tree explorer does not show personal workspace anymore
        getTreeExplorer().findElement(By.linkText("Domain"));
        checkMainTabs();
        // check we can still navigate to personal workspace
        page = page.switchToPersonalWorkspace();
        checkMainTabs();
        // check tree shows personal workspace
        getTreeExplorer().findElement(By.linkText("Administrator"));
        // clean up worklist
        getClipboard().findElement(By.linkText("Clear List")).click();
    }

    // second use case of NXP-9813
    @Test
    public void testNavigationBetweenHomeAndPersonalWorkspace() throws Exception {
        DocumentBasePage page = login();
        checkMainTabs();
        page = page.switchToPersonalWorkspace();
        // check tree shows personal workspace
        getTreeExplorer().findElement(By.linkText("Administrator"));
        page = page.goToHomePage();
        page = page.goToWorkspaces();
        checkMainTabs();
        // check tree shows personal workspace
        getTreeExplorer().findElement(By.linkText("Administrator"));
        page = page.switchToDocumentBase();
        // check tree shows document base
        getTreeExplorer().findElement(By.linkText("Domain"));
    }

    protected WebElement getClipboard() {
        return Locator.findElementWithTimeout(By.id("clipboardForm"));
    }

    protected WebElement getTreeExplorer() {
        return Locator.findElementWithTimeout(By.id("treeExplorer"));
    }

    protected void checkMainTabs() {
        DocumentBasePage page = asPage(DocumentBasePage.class);
        assertTrue(page.isMainTabSelected(page.documentManagementLink));
        assertFalse(page.isMainTabSelected(page.searchPageLink));
        assertFalse(page.isMainTabSelected(page.homePageLink));
    }

}
