/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nelson Silva
 *     Yannis JULIENNE
 */
package org.nuxeo.functionaltests.pages.admincenter.usermanagement;

import static org.junit.Assert.assertNotNull;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.admincenter.AdminCenterBasePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Nuxeo User and Groups Base page. (New one in the admin center)
 *
 * @since 5.4.2
 */
public class UsersGroupsBasePage extends AdminCenterBasePage {

    @FindBy(xpath = "//div[@id=\"nxw_adminCenterSubTabs_panel\"]/ul/li[@class=\"selected\"]/form/a")
    public WebElement selectedTab;

    @Required
    @FindBy(xpath = "//a[@id=\"nxw_UsersManager_form:nxw_UsersManager\"]")
    public WebElement usersTabLink;

    @Required
    @FindBy(xpath = "//a[@id=\"nxw_GroupsManager_form:nxw_GroupsManager\"]")
    public WebElement groupsTabLink;

    protected void clickOnLinkIfNotSelected(WebElement tabLink) {
        assertNotNull(tabLink);
        assertNotNull(selectedTab);

        if (!selectedTab.equals(tabLink)) {
            waitUntilEnabledAndClick(tabLink);
        }
    }

    public UsersGroupsBasePage(WebDriver driver) {
        super(driver);
    }

    /**
     * View the Users tab.
     */
    public UsersTabSubPage getUsersTab(boolean force) {
        if (force) {
            waitUntilEnabledAndClick(usersTabLink);
        } else {
            clickOnLinkIfNotSelected(usersTabLink);
        }
        return asPage(UsersTabSubPage.class);
    }

    public UsersTabSubPage getUsersTab() {
        return getUsersTab(false);
    }

    /**
     * View the Groups tab.
     */
    public GroupsTabSubPage getGroupsTab(boolean force) {
        if (force) {
            waitUntilEnabledAndClick(groupsTabLink);
        } else {
            clickOnLinkIfNotSelected(groupsTabLink);
        }
        return asPage(GroupsTabSubPage.class);
    }

    public GroupsTabSubPage getGroupsTab() {
        return getGroupsTab(false);
    }
}
