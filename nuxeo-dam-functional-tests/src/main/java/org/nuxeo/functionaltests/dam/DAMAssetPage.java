/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.functionaltests.dam;

import static org.junit.Assert.assertEquals;

import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * @since 5.7.3
 */
public class DAMAssetPage extends DocumentBasePage {

    public DAMAssetPage(WebDriver driver) {
        super(driver);
    }

    public String getAssetTitle() {
        return Locator.findElementWithTimeout(By.className("documentTitle")).getText();
    }

    public void checkAssetTitle(String expectedTitle) {
        assertEquals(expectedTitle, getAssetTitle());
    }

    public WebElement getBackToDAMLink() {
        return driver.findElement(By.xpath("//a/span[text()='Back to DAM']/.."));
    }
}
