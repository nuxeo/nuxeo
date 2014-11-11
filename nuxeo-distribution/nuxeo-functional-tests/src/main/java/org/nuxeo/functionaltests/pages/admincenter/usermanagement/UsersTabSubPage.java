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
 *     Antoine Taillefer
 */
package org.nuxeo.functionaltests.pages.admincenter.usermanagement;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Users Tab Page of the users & groups management (New one in the admin center)
 *
 * @since 5.4.2
 */
public class UsersTabSubPage extends UsersGroupsBasePage {

    @Required
    @FindBy(id = "usersListingView:createUserActionsForm:createUserButton")
    WebElement createNewUserLink;

    @FindBy(id = "usersListingView:searchForm:searchText")
    WebElement searchInput;

    @FindBy(id = "usersListingView:searchForm:searchButton")
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
        searchButton.submit();
        return asPage(UsersTabSubPage.class);
    }

    /**
     * Is the username was found in the last search result page.
     */
    public boolean isUserFound(String username) {
        try {
            findElementWithTimeout(By.linkText(username),
                    AbstractTest.LOAD_SHORT_TIMEOUT_SECONDS * 1000);
        } catch (NoSuchElementException e) {
            return false;
        }
        return true;
    }

    public UserViewTabSubPage viewUser(String username) {
        findElementWithTimeout(By.linkText(username)).click();
        return asPage(UserViewTabSubPage.class);
    }

}
