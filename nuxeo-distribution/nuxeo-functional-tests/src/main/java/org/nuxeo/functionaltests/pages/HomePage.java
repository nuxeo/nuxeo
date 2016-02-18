/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.functionaltests.pages;

import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 5.9.3
 */
public class HomePage extends DocumentBasePage {

    static final String USERS_GROUPS_LABEL = "Users & Groups";

    static final String COLLECTIONS_LABEL = "Collections";

    static final String SEARCHES_LABEL = "Searches";

    @Required
    @FindBy(id = "nxw_homeTabs_panel")
    protected WebElement menu;

    public HomePage(WebDriver driver) {
        super(driver);
    }

    /**
     * @since 8.2
     */
    public UsersGroupsHomePage goToUsersGroupsHomePage() {
        goTo(USERS_GROUPS_LABEL);
        return asPage(UsersGroupsHomePage.class);
    }

    public CollectionsPage goToCollections() {
        goTo(COLLECTIONS_LABEL);
        return asPage(CollectionsPage.class);
    }

    /**
     * @since 8.1
     */
    public HomePage goToSavedSearches() {
        goTo(SEARCHES_LABEL);
        return asPage(HomePage.class);
    }

    /**
     * @since 8.2
     */
    public void goTo(String tabLabel) {
        if (useAjaxTabs()) {
            AjaxRequestManager arm = new AjaxRequestManager(driver);
            arm.begin();
            menu.findElement(By.linkText(tabLabel)).click();
            arm.end();
        } else {
            menu.findElement(By.linkText(tabLabel)).click();
        }
    }

}
