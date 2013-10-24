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
package org.nuxeo.functionaltests;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Simple Diff tests
 * 
 * WARNING: Class copied from https://github.com/nuxeo/marketplace-diff/
 * Keep in sync with it.
 *
 * @since 5.7.2
 */
public class ITSimpleDiff extends AbstractTest {

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
        DocumentBasePage defaultDomain = login();

        DocumentBasePage workspacePage = initRepository(defaultDomain);

        // Create test File 1
        final String DESCRIPTION_1 = "" + System.currentTimeMillis();
        DocumentBasePage newFile = createFile(workspacePage, "Test file 1",
                DESCRIPTION_1, false, null, null, null);

        workspacePage = newFile.getNavigationSubPage().goToDocument(
                "Test Workspace");

        // Create test File 2
        final String DESCRIPTION_2 = "" + System.currentTimeMillis();
        newFile = createFile(workspacePage, "Test file 2", DESCRIPTION_2,
                false, null, null, null);

        workspacePage = newFile.getNavigationSubPage().goToDocument(
                "Test Workspace");

        WebElement document_content = findElementWithTimeout(By.xpath("//form[@id=\"document_content\"]"));

        List<WebElement> trelements = document_content.findElement(
                By.tagName("tbody")).findElements(By.tagName("tr"));

        assertTrue(trelements != null && trelements.size() == 2);

        trelements.get(0).findElement(By.xpath("td/input[@type=\"checkbox\"]")).click();
        trelements.get(1).findElement(By.xpath("td/input[@type=\"checkbox\"]")).click();

        findElementWaitUntilEnabledAndClick(By.xpath("//input[@value=\"Compare\"]"));

        WebElement description1 = findElementWithTimeout(By.xpath("//div[contains(.,'"
                + DESCRIPTION_1 + "')]"));

        assertTrue(description1 != null);

        WebElement description2 = findElementWithTimeout(By.xpath("//div[contains(.,'"
                + DESCRIPTION_1 + "')]"));

        assertTrue(description2 != null);

        cleanRepository(workspacePage);

        logout();
    }

}
