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
package org.nuxeo.functionaltests.pages.usermanagement.compat;

import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class UsersTabSubPage extends UsersGroupsBasePage {

    @Required
    @FindBy(linkText = "Create a new user")
    WebElement createNewUserLink;

    @FindBy(name = "searchForm:searchText")
    WebElement searchInput;

    @FindBy(name = "searchForm:searchButton")
    WebElement searchButton;

    public UsersTabSubPage(WebDriver driver) {
        super(driver);
    }

    public UserCreationFormPage getUserCreatePage() {
        createNewUserLink.click();
        return asPage(UserCreationFormPage.class);
    }

    public UsersTabSubPage searchUser(String query) {
        searchInput.clear();
        searchInput.sendKeys(query);
        searchButton.click();
        return asPage(UsersTabSubPage.class);
    }

    /**
     * Is the username was found in the last search result page.
     */
    public boolean isUserFound(String username) {
        try {
            driver.findElement(By.linkText(username));
        } catch (NoSuchElementException e) {
            return false;
        }
        return true;
    }

    public UserViewTabSubPage viewUser(String username) {
        driver.findElement(By.linkText(username)).click();
        return asPage(UserViewTabSubPage.class);
    }

}
