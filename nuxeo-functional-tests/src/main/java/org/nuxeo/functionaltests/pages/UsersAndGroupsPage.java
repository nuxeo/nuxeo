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
 *     Benoit Delbosc
 */
package org.nuxeo.functionaltests.pages;

import static junit.framework.Assert.assertNotNull;

import org.nuxeo.functionaltests.pages.tabs.UsersTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Nuxeo User and Groups page.
 */
public class UsersAndGroupsPage extends DocumentBasePage {

    @FindBy(xpath = "//div[@class=\"tabsBar\"]/form/ul/li/a[text()=\"Users\"]")
    public WebElement usersTabLink;


    public UsersAndGroupsPage(WebDriver driver) {
        super(driver);
        assertNotNull(driver.findElement(By.id("createUserActionsForm")));
    }
    /**
     * View the Users tab.
     *
     * @return
     */
    public UsersTabSubPage getUsersTab() {
        clickOnLinkIfNotSelected(usersTabLink);
        return asPage(UsersTabSubPage.class);
    }


}
