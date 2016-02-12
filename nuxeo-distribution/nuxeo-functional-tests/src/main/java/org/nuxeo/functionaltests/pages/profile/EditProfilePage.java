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
import org.nuxeo.functionaltests.pages.UserHomePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 8.2
 */
public class EditProfilePage extends AbstractPage {

    @Required
    @FindBy(id = "editUser:nxl_user_1:nxw_email_1")
    WebElement emailInput;

    @Required
    @FindBy(id = "editUser:nxl_userprofile_1:nxw_birthdate_1InputDate")
    WebElement birthDateInput;

    @Required
    @FindBy(id = "editUser:nxl_userprofile_1:nxw_phonenumber_1")
    WebElement phoneNumberInput;

    @Required
    @FindBy(xpath = "//input[@value=\"Save\"]")
    WebElement saveButton;

    public EditProfilePage(WebDriver driver) {
        super(driver);
    }

    public EditProfilePage setEmail(String email) {
        emailInput.sendKeys(email);
        return asPage(EditProfilePage.class);
    }

    public EditProfilePage setBirthDate(String birthDate) {
        birthDateInput.sendKeys(birthDate);
        return asPage(EditProfilePage.class);
    }

    public EditProfilePage setPhoneNumber(String phoneNumber) {
        phoneNumberInput.sendKeys(phoneNumber);
        return asPage(EditProfilePage.class);
    }

    public UserHomePage saveProfile() {
        saveButton.click();
        return asPage(UserHomePage.class);
    }
}
