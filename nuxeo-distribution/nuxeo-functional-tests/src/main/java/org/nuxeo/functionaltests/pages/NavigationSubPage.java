/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Sun Seng David TAN <stan@nuxeo.com>
 */
package org.nuxeo.functionaltests.pages;

import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author Sun Seng David TAN <stan@nuxeo.com>
 *
 */
public class NavigationSubPage extends AbstractPage {

    @Required
    @FindBy(xpath = "//div[@class=\"userClipboardContent\"]/div[@class=\"menu\"]")
    WebElement navigationTree;

    /**
     * @param driver
     */
    public NavigationSubPage(WebDriver driver) {
        super(driver);
    }

    public DocumentBasePage goToDocument(String docTitle) {
        navigationTree.findElement(By.linkText(docTitle)).click();
        // wait for page load after click
        findElementWithTimeout(By.className("userActions"));
        return asPage(DocumentBasePage.class);
    }

}
