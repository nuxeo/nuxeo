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
    @FindBy(xpath="//form[@id=\"editUser\"]//input[@value=\"Save\"]")
    WebElement saveButton;

    public UserEditFormPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Edit a user, only update non null value.
     *
     * @param firstname
     * @param lastname
     * @param company
     * @param email
     * @param password
     * @param group
     * @return
     * @throws NoSuchElementException
     */
    public UserViewTabSubPage editUser(String firstname, String lastname,
            String company, String email, String group)
            throws NoSuchElementException {
        updateInput(firstnameInput, firstname);
        updateInput(lastnameInput, lastname);
        updateInput(companyInput, company);
        updateInput(emailInput, email);
        if (group != null) {
            groupInput.sendKeys(group);
            WebElement ajaxUserListElement = findElementWithTimeout(By.xpath("//table[@id='editUser:nxl_user:nxw_groups_suggestionBox:suggest']/tbody/tr[1]/td[2]"));
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
