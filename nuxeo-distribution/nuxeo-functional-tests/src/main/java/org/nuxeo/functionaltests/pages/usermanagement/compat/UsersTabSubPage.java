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
