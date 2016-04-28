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
 *     Antoine Taillefer
 *     Yannis JULIENNE
 */
package org.nuxeo.functionaltests.pages.admincenter.usermanagement;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * View user details (New one in the admin center)
 *
 * @since 5.4.2
 */
public class UserViewTabSubPage extends UsersGroupsBasePage {

    @Required
    @FindBy(linkText = "View")
    WebElement viewUserTab;

    @FindBy(linkText = "Delete")
    WebElement deleteUserLink;

    @FindBy(linkText = "Edit")
    WebElement editLink;

    @FindBy(linkText = "Change Password")
    WebElement changePasswordLink;

    @FindBy(xpath = "//div[@id='nxw_userCenterSubTabs_tab_content']//h1")
    WebElement currentUserName;

    @FindBy(id = "viewUserView:viewUser:nxl_gridUserLayout:nxw_userPanelLeft_panel")
    WebElement viewUserPanel;

    public UserViewTabSubPage(WebDriver driver) {
        super(driver);
    }

    public UsersTabSubPage deleteUser() {
        deleteUserLink.click();
        Alert alert = driver.switchTo().alert();
        assertEquals("Delete user?", alert.getText());
        alert.accept();
        return asPage(UsersTabSubPage.class);
    }

    public UserEditFormPage getEditUserTab() {
        editLink.click();
        return asPage(UserEditFormPage.class);
    }

    public UserChangePasswordFormPage getChangePasswordUserTab() {
        changePasswordLink.click();
        return asPage(UserChangePasswordFormPage.class);
    }

    public UsersTabSubPage backToTheList() {
        findElementWaitUntilEnabledAndClick(By.linkText("Back to the List"));
        return asPage(UsersTabSubPage.class);
    }

    /**
     * @since 6.0
     */
    public void checkUserName(String expectedName) {
        assertEquals(expectedName, getCurrentUserName().getText());
    }

    /**
     * @since 6.0
     */
    public WebElement getCurrentUserName() {
        return currentUserName;
    }

    /**
     * @since 8.3
     */
    public List<String> getGroupLabels() {
        List<WebElement> goupElements = viewUserPanel.findElements(By.className("group"));
        List<String> groups = new ArrayList<>();
        for (WebElement groupElement : goupElements) {
            groups.add(groupElement.getText());
        }
        return groups;
    }
}
