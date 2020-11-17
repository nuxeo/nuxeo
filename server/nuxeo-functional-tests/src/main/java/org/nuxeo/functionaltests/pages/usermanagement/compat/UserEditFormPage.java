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

import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * View user details
 */
public class UserEditFormPage extends UsersGroupsBasePage {

    @Required
    @FindBy(id = "editUser:nxl_user:nxw_firstname")
    WebElement firstnameInput;

    @Required
    @FindBy(id = "editUser:nxl_user:nxw_lastname")
    WebElement lastnameInput;

    @Required
    @FindBy(id = "editUser:nxl_user:nxw_company")
    WebElement companyInput;

    @Required
    @FindBy(id = "editUser:nxl_user:nxw_email")
    WebElement emailInput;

    @Required
    @FindBy(id = "editUser:nxl_user:nxw_groups_suggest")
    WebElement groupInput;

    @Required
    @FindBy(xpath = "//form[@id=\"editUser\"]//input[@value=\"Save\"]")
    WebElement saveButton;

    public UserEditFormPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Edit a user, only update non null value.
     */
    public UserViewTabSubPage editUser(String firstname, String lastname, String company, String email, String group)
            throws NoSuchElementException {
        updateInput(firstnameInput, firstname);
        updateInput(lastnameInput, lastname);
        updateInput(companyInput, company);
        updateInput(emailInput, email);
        if (group != null) {
            groupInput.sendKeys(group);
            WebElement ajaxUserListElement = findElementWithTimeout(
                    By.xpath("//table[@id='editUser:nxl_user:nxw_groups_suggestionBox:suggest']/tbody/tr[1]/td[2]"));
            ajaxUserListElement.click();
        }
        saveButton.click();
        return asPage(UserViewTabSubPage.class);
    }

    private void updateInput(WebElement elem, String value) {
        if (value != null) {
            elem.clear();
            elem.sendKeys(value);
        }
    }

}
