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
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

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
