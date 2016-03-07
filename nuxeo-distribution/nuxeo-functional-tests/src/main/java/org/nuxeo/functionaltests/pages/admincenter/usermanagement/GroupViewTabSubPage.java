/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertEquals;

import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * View group details
 *
 * @since 7.2
 */
public class GroupViewTabSubPage extends UsersGroupsBasePage {

    @Required
    @FindBy(xpath = "//div[@id='nxw_adminCenterSubTabs_tab_content']//h1")
    WebElement groupName;

    @Required
    @FindBy(linkText = "View")
    WebElement viewGroupTab;

    @FindBy(linkText = "Delete")
    WebElement deleteGroupLink;

    @FindBy(linkText = "Edit")
    WebElement editLink;

    public GroupViewTabSubPage(WebDriver driver) {
        super(driver);
    }

    public GroupsTabSubPage deleteGroup() {
        deleteGroupLink.click();
        Alert alert = driver.switchTo().alert();
        assertEquals("Delete group?", alert.getText());
        alert.accept();
        return asPage(GroupsTabSubPage.class);
    }

    public GroupEditFormPage getEditGroupTab() {
        editLink.click();
        return asPage(GroupEditFormPage.class);
    }

    public GroupsTabSubPage backToTheList() {
        findElementWaitUntilEnabledAndClick(By.linkText("Back to the List"));
        return asPage(GroupsTabSubPage.class);
    }

    /**
     * @since 8.2
     */
    public String getGroupName() {
        return groupName.getText();
    }

}
