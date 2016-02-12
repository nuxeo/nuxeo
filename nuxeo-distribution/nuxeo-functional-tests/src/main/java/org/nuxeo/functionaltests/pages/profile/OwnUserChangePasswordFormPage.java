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
 *     Thomas Roger
 *
 */
package org.nuxeo.functionaltests.pages.profile;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Change password sub tab page (From profile page)
 *
 * @since 7.3
 */
public class OwnUserChangePasswordFormPage extends AbstractPage {

    @Required
    @FindBy(id = "editUserPassword:nxl_profile_password:nxw_profilePasswordMatcher_firstPassword")
    WebElement firstPasswordInput;

    @Required
    @FindBy(id = "editUserPassword:nxl_profile_password:nxw_profilePasswordMatcher_secondPassword")
    WebElement secondPasswordInput;

    @Required
    @FindBy(id = "editUserPassword:nxl_profile_password:nxw_profilePasswordMatcher_oldPassword")
    WebElement oldPasswordInput;

    @Required
    @FindBy(xpath = "//input[@value=\"Save\"]")
    WebElement saveButton;

    public OwnUserChangePasswordFormPage(WebDriver driver) {
        super(driver);
    }

    public OwnUserChangePasswordFormPage changePassword(String oldPassword, String password) {
        firstPasswordInput.clear();
        firstPasswordInput.sendKeys(password);
        secondPasswordInput.clear();
        secondPasswordInput.sendKeys(password);
        oldPasswordInput.clear();
        oldPasswordInput.sendKeys(oldPassword);
        saveButton.click();
        return asPage(OwnUserChangePasswordFormPage.class);
    }

}
