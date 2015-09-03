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
package org.nuxeo.ftest.formsLayoutDemo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.formsLayoutDemo.page.HomePage;
import org.nuxeo.functionaltests.formsLayoutDemo.page.Page;
import org.openqa.selenium.By;

/**
 * Tests every menu item.
 *
 * @since 7.4
 */
public class ITMenuTest extends AbstractTest {

    private static final Log log = LogFactory.getLog(ITMenuTest.class);

    @Test
    public void testMenuItems() {
        HomePage p = get(HomePage.URL, HomePage.class);
        // iterate on introspected menu items
        Map<String, List<String>> menuItems = p.getMenuItems();
        assertNotNull(menuItems);
        assertNotEquals(menuItems.size(), 0);
        for (Map.Entry<String, List<String>> menuItem : menuItems.entrySet()) {
            for (String title : menuItem.getValue()) {
                if ("Single user".equals(title)) {
                    checkPage(menuItem.getKey(), title);
                }
            }
        }
    }

    protected void checkPage(String boxTitle, String title) {
        assertFalse(StringUtils.isBlank(title));
        log.info(String.format("Checking page '%s'", title));
        get(HomePage.URL, HomePage.class);
        assertNotNull(driver.findElementByLinkText(title));
        assertTrue(driver.findElementByLinkText(title).isDisplayed());
        driver.findElementByLinkText(title).click();
        // ensure navigation did not crash, and menu item is open
        checkNoCrash(title);
        assertTrue(driver.findElementByLinkText(boxTitle).isDisplayed());
        // check reference and preview pages do not crash
        Page page = asPage(Page.class);
        page.goToReferenceTab();
        checkNoCrash(title);
        if (page.hasPreviewTab()) {
            page.goToPreviewTab();
            checkNoCrash(title);
        }
        page.goToOverviewTab();
        checkNoCrash(title);
    }

    protected void checkNoCrash(String title) {
        Locator.waitUntilElementPresent(By.linkText(title));
    }

}