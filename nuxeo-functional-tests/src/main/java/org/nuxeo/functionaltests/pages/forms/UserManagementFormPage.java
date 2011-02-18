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
 *     Sun Seng David TAN
 *     Florent Guillaume
 */
package org.nuxeo.functionaltests.pages.forms;

import org.nuxeo.functionaltests.finders.ElementNotFoundException;
import org.nuxeo.functionaltests.finders.FindElementUntil;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Nuxeo DM user management page.
 */
public class UserManagementFormPage extends AbstractPage {

    @FindBy(id = "createUser:nxl_user:nxw_username")
    WebElement usernameInput;

    @FindBy(id = "createUser:nxl_user:nxw_firstname")
    WebElement firstnameInput;

    @FindBy(id = "createUser:nxl_user:nxw_lastname")
    WebElement lastnameInput;

    @FindBy(id = "createUser:nxl_user:nxw_company")
    WebElement companyInput;

    @FindBy(id = "createUser:nxl_user:nxw_email")
    WebElement emailInput;

    @FindBy(id = "createUser:nxl_user:nxw_firstPassword")
    WebElement firstPasswordInput;

    @FindBy(id = "createUser:nxl_user:nxw_secondPassword")
    WebElement secondPasswordInput;

    @FindBy(id = "createUser:nxl_user:nxw_groups_suggest")
    WebElement groupInput;

    @FindBy(id = "createUser:button_create")
    WebElement createButton;

    public UserManagementFormPage(WebDriver driver) {
        super(driver);
    }

    public void createUser(String username, String firstname, String lastname,
            String company, String email, String password, String group)
            throws ElementNotFoundException {
        usernameInput.sendKeys(username);
        firstnameInput.sendKeys(firstname);
        lastnameInput.sendKeys(lastname);
        companyInput.sendKeys(company);
        emailInput.sendKeys(email);
        firstPasswordInput.sendKeys(password);
        secondPasswordInput.sendKeys(password);
        groupInput.sendKeys(group);
        WebElement ajaxUserListElement = new FindElementUntil(
                driver,
                By.xpath("//table[@id='createUser:nxl_user:nxw_groups_suggestionBox:suggest']/tbody/tr[1]/td[2]")).find();
        ajaxUserListElement.click();
        driver.findElement(By.id("createUser:button_create")).click();
    }

}
