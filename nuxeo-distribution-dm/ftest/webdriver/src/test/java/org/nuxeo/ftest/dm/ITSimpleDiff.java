/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Guillaume Renard
 */
package org.nuxeo.ftest.dm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.Test;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.pages.DocumentBasePage;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Simple Diff tests
 *
 * WARNING: Class copied from https://github.com/nuxeo/marketplace-diff/ Keep in
 * sync with it.
 *
 * @since 5.7.2
 */
public class ITSimpleDiff extends AbstractTest {

    private final static String LEFT_DESCRIPTION_XPATH = "//span[@id='document_diff:nxl_dublincore:nxw_dublincore_description_left:nxw_dublincore_description:nxw_text']";

    private final static String RIGHT_DESCRIPTION_XPATH = "//span[@id='document_diff:nxl_dublincore:nxw_dublincore_description_right:nxw_dublincore_description:nxw_text_1']";

    private final static String WORKSPACE_TITLE = "WorkspaceTitle_"
            + new Date().getTime();

    /**
     * Create a workspace with 2 files having a different description and then
     * perform a "Compare" on them. Check that the diff on description is
     * detected.
     *
     * @throws Exception
     *
     * @since 5.7.2
     */
    @Test
    public void testDiffDescription() throws Exception {
        DocumentBasePage documentBasePage = login();

        documentBasePage = documentBasePage.getNavigationSubPage().goToDocument(
                "Workspaces");
        DocumentBasePage workspacePage = createWorkspace(documentBasePage,
                WORKSPACE_TITLE, null);

        // Create test File 1
        final String DESCRIPTION_1 = "" + System.currentTimeMillis();
        DocumentBasePage newFile = createFile(workspacePage, "Test file 1",
                DESCRIPTION_1, false, null, null, null);

        workspacePage = newFile.getNavigationSubPage().goToDocument(
                WORKSPACE_TITLE);

        // Create test File 2
        final String DESCRIPTION_2 = "" + System.currentTimeMillis();
        // Make sure the descriptions are different
        assertFalse(DESCRIPTION_1.equals(DESCRIPTION_2));
        newFile = createFile(workspacePage, "Test file 2", DESCRIPTION_2,
                false, null, null, null);

        workspacePage = newFile.getNavigationSubPage().goToDocument(
                WORKSPACE_TITLE);

        WebElement document_content = Locator.findElementWithTimeout(By.xpath("//form[@id=\"document_content\"]"));

        List<WebElement> trelements = document_content.findElement(
                By.tagName("tbody")).findElements(By.tagName("tr"));

        assertTrue(trelements != null && trelements.size() == 2);

        trelements.get(0).findElement(By.xpath("td/input[@type=\"checkbox\"]")).click();
        trelements.get(1).findElement(By.xpath("td/input[@type=\"checkbox\"]")).click();

        Locator.findElementWaitUntilEnabledAndClick(By.xpath("//input[@value=\"Compare\"]"));

        WebElement description1 = Locator.findElementWithTimeout(By.xpath(LEFT_DESCRIPTION_XPATH));
        String description1Text = description1.getText();
        assertTrue(description1Text != null
                && description1Text.equals(DESCRIPTION_1));

        WebElement description2 = Locator.findElementWithTimeout(By.xpath(RIGHT_DESCRIPTION_XPATH));

        String description2Text = description2.getText();
        assertTrue(description2Text != null
                && description2Text.equals(DESCRIPTION_2));

        deleteWorkspace(documentBasePage, WORKSPACE_TITLE);

        logout();
    }

}
