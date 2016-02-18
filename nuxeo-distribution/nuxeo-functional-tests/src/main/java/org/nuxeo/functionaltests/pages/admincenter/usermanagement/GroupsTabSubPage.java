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
 *     Nelson Silva
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
 * Groups Tab Page of the users & groups management
 *
 * @since 7.2
 */
public class GroupsTabSubPage extends UsersGroupsBasePage {

    @FindBy(id = "groupsListingView:createGroupActionsForm:createGroupButton")
    WebElement createNewGroupLink;

    @Required
    @FindBy(id = "groupsListingView:searchForm:searchText")
    WebElement searchInput;

    @Required
    @FindBy(id = "groupsListingView:searchForm:searchButton")
    WebElement searchButton;

    public GroupsTabSubPage(WebDriver driver) {
        super(driver);
    }

    public GroupCreationFormPage getGroupCreatePage() {
        createNewGroupLink.click();
        return asPage(GroupCreationFormPage.class);
    }

    public GroupsTabSubPage searchGroup(String query) {
        searchInput.clear();
        searchInput.sendKeys(query);
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        searchButton.click();
        arm.end();
        return asPage(GroupsTabSubPage.class);
    }

    /**
     * Checks if the group was found in the last search result page.
     */
    public boolean isGroupFound(String groupname) {
        try {
            findElementWithTimeout(By.linkText(groupname), AbstractTest.LOAD_SHORT_TIMEOUT_SECONDS * 1000);
        } catch (NoSuchElementException e) {
            return false;
        }
        return true;
    }

    public GroupViewTabSubPage viewGroup(String groupname) {
        findElementWithTimeout(By.linkText(groupname)).click();
        return asPage(GroupViewTabSubPage.class);
    }

}
