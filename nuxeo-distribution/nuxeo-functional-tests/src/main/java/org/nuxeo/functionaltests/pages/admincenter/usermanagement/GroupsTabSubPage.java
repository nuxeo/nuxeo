/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nelson Silva
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
 * Groups Tab Page of the users & groups management
 *
 * @since 7.2
 */
public class GroupsTabSubPage extends UsersGroupsBasePage {

    @Required
    @FindBy(id = "groupsListingView:createGroupActionsForm:createGroupButton")
    WebElement createNewGroupLink;

    @FindBy(id = "groupsListingView:searchForm:searchText")
    WebElement searchInput;

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
        searchButton.submit();
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
