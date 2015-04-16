/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Maxime Hilaire
 */
package org.nuxeo.functionaltests.pages;

import org.nuxeo.functionaltests.Required;
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
    @FindBy(id = "editUserPassword:nxl_user_2:nxw_passwordMatcher_firstPassword")
    WebElement firstPasswordInput;

    @Required
    @FindBy(id = "editUserPassword:nxl_user_2:nxw_passwordMatcher_secondPassword")
    WebElement secondPasswordInput;

    @Required
    @FindBy(xpath = "//input[@value=\"Save\"]")
    WebElement saveButton;

    public OwnUserChangePasswordFormPage(WebDriver driver) {
        super(driver);
    }

    public OwnUserChangePasswordFormPage changePassword(String password) {
        firstPasswordInput.clear();
        firstPasswordInput.sendKeys(password);
        secondPasswordInput.clear();
        secondPasswordInput.sendKeys(password);
        saveButton.click();
        return asPage(OwnUserChangePasswordFormPage.class);
    }

}
