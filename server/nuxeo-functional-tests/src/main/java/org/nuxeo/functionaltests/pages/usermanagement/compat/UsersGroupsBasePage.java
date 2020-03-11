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

import static org.junit.Assert.assertNotNull;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Nuxeo User and Groups Base page.
 */
public class UsersGroupsBasePage extends AbstractPage {
    @FindBy(xpath = "//div[@class=\"tabsBar\"]/form/ul/li[@class=\"selected\"]/a[text()=\"Users\" or text()=\"Groups\"]")
    public WebElement selectedTab;

    @Required
    @FindBy(xpath = "//div[@class=\"tabsBar\"]/form/ul/li/a[text()=\"Users\"]")
    public WebElement usersTabLink;

    protected void clickOnLinkIfNotSelected(WebElement tabLink) {
        assertNotNull(tabLink);
        assertNotNull(selectedTab);

        if (!selectedTab.equals(tabLink)) {
            tabLink.click();
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
            usersTabLink.click();
        } else {
            clickOnLinkIfNotSelected(usersTabLink);
        }
        return asPage(UsersTabSubPage.class);
    }

    public UsersTabSubPage getUsersTab() {
        return getUsersTab(false);
    }

}
