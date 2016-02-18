/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 *     Antoine Taillefer
 */
package org.nuxeo.functionaltests.pages.admincenter.usermanagement;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.AjaxRequestManager;
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

    @FindBy(id = "usersListingView:createUserActionsForm:createUserButton")
    WebElement createNewUserLink;

    @Required
    @FindBy(id = "usersListingView:searchForm:searchText")
    WebElement searchInput;

    @Required
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
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        searchButton.click();
        arm.end();
        return asPage(UsersTabSubPage.class);
    }

    /**
     * Is the username was found in the last search result page.
     */
    public boolean isUserFound(String username) {
        try {
            findElementWithTimeout(By.linkText(username), AbstractTest.LOAD_SHORT_TIMEOUT_SECONDS * 1000);
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
