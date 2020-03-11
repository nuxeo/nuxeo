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
 *     Sun Seng David TAN
 *     Florent Guillaume
 */
package org.nuxeo.functionaltests.pages.usermanagement.compat;

import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Nuxeo DM user management page.
 */
public class UserCreationFormPage extends UsersGroupsBasePage {

    @Required
    @FindBy(id = "createUser:nxl_user:nxw_username")
    WebElement usernameInput;

    @Required
    @FindBy(id = "createUser:nxl_user:nxw_firstname")
    WebElement firstnameInput;

    @Required
    @FindBy(id = "createUser:nxl_user:nxw_lastname")
    WebElement lastnameInput;

    @Required
    @FindBy(id = "createUser:nxl_user:nxw_company")
    WebElement companyInput;

    @Required
    @FindBy(id = "createUser:nxl_user:nxw_email")
    WebElement emailInput;

    @Required
    @FindBy(id = "createUser:nxl_user:nxw_firstPassword")
    WebElement firstPasswordInput;

    @Required
    @FindBy(id = "createUser:nxl_user:nxw_secondPassword")
    WebElement secondPasswordInput;

    @Required
    @FindBy(id = "createUser:nxl_user:nxw_groups_suggest")
    WebElement groupInput;

    @Required
    @FindBy(id = "createUser:button_create")
    WebElement createButton;

    public UserCreationFormPage(WebDriver driver) {
        super(driver);
    }

    public UsersGroupsBasePage createUser(String username, String firstname, String lastname, String company,
            String email, String password, String group) throws NoSuchElementException {
        usernameInput.sendKeys(username);
        firstnameInput.sendKeys(firstname);
        lastnameInput.sendKeys(lastname);
        companyInput.sendKeys(company);
        emailInput.sendKeys(email);
        firstPasswordInput.sendKeys(password);
        secondPasswordInput.sendKeys(password);
        groupInput.sendKeys(group);
        WebElement ajaxUserListElement = findElementWithTimeout(
                By.xpath("//table[@id='createUser:nxl_user:nxw_groups_suggestionBox:suggest']/tbody/tr[1]/td[2]"));
        ajaxUserListElement.click();
        createButton.click();
        return asPage(UsersGroupsBasePage.class);
    }

}
