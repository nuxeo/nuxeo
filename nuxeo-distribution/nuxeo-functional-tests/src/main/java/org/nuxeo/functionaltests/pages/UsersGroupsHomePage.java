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
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests.pages;

import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.GroupsTabSubPage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersTabSubPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Users & Groups page visible in "Home" main tab.
 *
 * @since 8.2
 */
public class UsersGroupsHomePage extends HomePage {

    @Required
    @FindBy(id = "nxw_UsersHome_form:nxw_UsersHome")
    public WebElement usersTabLink;

    @Required
    @FindBy(id = "nxw_GroupsHome_form:nxw_GroupsHome")
    public WebElement groupsTabLink;

    public UsersGroupsHomePage(WebDriver driver) {
        super(driver);
    }

    public UsersTabSubPage getUsersTab() {
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        usersTabLink.click();
        arm.end();
        return asPage(UsersTabSubPage.class);
    }

    public GroupsTabSubPage getGroupsTab() {
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        groupsTabLink.click();
        arm.end();
        return asPage(GroupsTabSubPage.class);
    }

}
