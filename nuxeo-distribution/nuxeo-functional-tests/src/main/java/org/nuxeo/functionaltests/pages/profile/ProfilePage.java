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
 *     Maxime HILAIRE
 *     Thomas Roger
 *
 */
package org.nuxeo.functionaltests.pages.profile;

import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * @since 8.2
 */
public class ProfilePage extends AbstractPage {

    public ProfilePage(final WebDriver driver) {
        super(driver);
    }

    public OwnUserChangePasswordFormPage getChangePasswordUserTab() {
        WebElement actionsLink = findElementWithTimeout(By.id("userProfileDropDownMenu"));
        actionsLink.click();
        WebElement changePasswordLink = findElementWithTimeout(By.id("userProfileButtons:changePasswordButton"));
        changePasswordLink.click();
        return asPage(OwnUserChangePasswordFormPage.class);
    }

    public EditProfilePage getEditProfilePage() {
        WebElement actionsLink = findElementWithTimeout(By.id("userProfileDropDownMenu"));
        actionsLink.click();
        WebElement editLink = findElementWithTimeout(By.id("userProfileButtons:editUserButton"));
        editLink.click();
        return asPage(EditProfilePage.class);
    }
}
