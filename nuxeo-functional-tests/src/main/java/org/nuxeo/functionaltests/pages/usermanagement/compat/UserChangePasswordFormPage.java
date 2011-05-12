/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Benoit Delbosc
 */
package org.nuxeo.functionaltests.pages.usermanagement.compat;

import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class UserChangePasswordFormPage extends UsersGroupsBasePage {

    @Required
    @FindBy(id = "editUser:nxl_user:nxw_firstPassword")
    WebElement firstPasswordInput;

    @Required
    @FindBy(id = "editUser:nxl_user:nxw_secondPassword")
    WebElement secondPasswordInput;

    @Required
    @FindBy(xpath = "//form[@id=\"editUser\"]//input[@value=\"Save\"]")
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
