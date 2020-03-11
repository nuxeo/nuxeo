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
package org.nuxeo.functionaltests.pages.admincenter.usermanagement;

import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Change password sub tab page (New user management in the admin center)
 *
 * @since 5.4.2
 */
public class UserChangePasswordFormPage extends UsersGroupsBasePage {

    @Required
    @FindBy(id = "viewUserView:editUserPassword:nxl_user_1:nxw_passwordMatcher_firstPassword")
    WebElement firstPasswordInput;

    @Required
    @FindBy(id = "viewUserView:editUserPassword:nxl_user_1:nxw_passwordMatcher_secondPassword")
    WebElement secondPasswordInput;

    @Required
    @FindBy(xpath = "//input[@value=\"Save\"]")
    WebElement saveButton;

    public UserChangePasswordFormPage(WebDriver driver) {
        super(driver);
    }

    public UserViewTabSubPage changePassword(String password) {
        firstPasswordInput.clear();
        firstPasswordInput.sendKeys(password);
        secondPasswordInput.clear();
        secondPasswordInput.sendKeys(password);
        saveButton.click();
        return asPage(UserViewTabSubPage.class);
    }

}
