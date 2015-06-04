/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ftest.cap;

import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.tabs.ContentTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        page = page.swithToPersonalWorkspace();
        checkMainTabs();
        // check tree shows personal workspace
        getTreeExplorer().findElement(By.linkText("Administrator"));
        getClipboard().findElement(By.linkText(docTitle)).click();
        // check that tree explorer does not show personal workspace anymore
        getTreeExplorer().findElement(By.linkText("Default domain"));
        checkMainTabs();
        // check we can still navigate to personal workspace
        page = page.swithToPersonalWorkspace();
        checkMainTabs();
        // check tree shows personal workspace
        getTreeExplorer().findElement(By.linkText("Administrator"));
        // clean up worklist
        getClipboard().findElement(By.linkText("Clear list")).click();
    }

    // second use case of NXP-9813
    @Test
    public void testNavigationBetweenHomeAndPersonalWorkspace() throws Exception {
        DocumentBasePage page = login();
        checkMainTabs();
        page = page.swithToPersonalWorkspace();
        // check tree shows personal workspace
        getTreeExplorer().findElement(By.linkText("Administrator"));
        page = page.goToHomePage();
        page = page.getDocumentManagement();
        checkMainTabs();
        // check tree shows personal workspace
        getTreeExplorer().findElement(By.linkText("Administrator"));
        page = page.swithToDocumentBase();
        // check tree shows document base
        getTreeExplorer().findElement(By.linkText("Default domain"));
    }

    protected WebElement getClipboard() {
        return Locator.findElement(By.id("clipboardForm"));
    }

    protected WebElement getTreeExplorer() {
        return Locator.findElement(By.id("treeExplorer"));
    }

    protected void checkMainTabs() {
        DocumentBasePage page = asPage(DocumentBasePage.class);
        assertTrue(page.isMainTabSelected(page.documentManagementLink));
        assertFalse(page.isMainTabSelected(page.searchPageLink));
        assertFalse(page.isMainTabSelected(page.homePageLink));
    }

}
