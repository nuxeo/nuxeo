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
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test main tabs management.
 *
 * @since 7.3
 */
public class ITMainTabsTest extends AbstractTest {

    /**
     * Non-regression test for NXP-17207
     */
    @Test
    public void testSearchTabSelection() throws Exception {
        DocumentBasePage documentBasePage = login();
        assertTrue(documentBasePage.isMainTabSelected(documentBasePage.documentManagementLink));
        assertFalse(documentBasePage.isMainTabSelected(documentBasePage.searchPageLink));
        documentBasePage.goToSearchPage();
        assertFalse(documentBasePage.isMainTabSelected(documentBasePage.documentManagementLink));
        assertTrue(documentBasePage.isMainTabSelected(documentBasePage.searchPageLink));
        // click on first search result, at least user workspace should be there
        WebElement wsLink = Locator.findElement(By.cssSelector("span[class='documentTitle']"));
        wsLink.click();
        documentBasePage = AbstractTest.asPage(DocumentBasePage.class);
        // check that workspace main tab is now selected
        assertTrue(documentBasePage.isMainTabSelected(documentBasePage.documentManagementLink));
        assertFalse(documentBasePage.isMainTabSelected(documentBasePage.searchPageLink));
    }
}
