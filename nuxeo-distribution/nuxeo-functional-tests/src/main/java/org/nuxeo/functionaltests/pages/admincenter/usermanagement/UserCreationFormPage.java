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
 *     Antoine Taillefer
 */
package org.nuxeo.functionaltests.pages.admincenter.usermanagement;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Nuxeo DM user management creation form page. (New one in the admin center)
 *
 * @since 5.4.2
 */
public class UserCreationFormPage extends UsersGroupsBasePage {

    @FindBy(name = "createUserView:createUser:nxl_user:nxw_passwordMatcher_immediate_creation")
    List<WebElement> immediateCreation;

    @FindBy(id = "createUserView:createUser:nxl_user:nxw_username")
    WebElement usernameInput;

    @FindBy(id = "createUserView:createUser:nxl_user:nxw_firstname")
    WebElement firstnameInput;

    @FindBy(id = "createUserView:createUser:nxl_user:nxw_lastname")
    WebElement lastnameInput;

    @FindBy(id = "createUserView:createUser:nxl_user:nxw_company")
    WebElement companyInput;

    @FindBy(id = "createUserView:createUser:nxl_user:nxw_email")
    WebElement emailInput;

    @FindBy(id = "createUserView:createUser:nxl_user:nxw_passwordMatcher_firstPassword")
    WebElement firstPasswordInput;

    @FindBy(id = "createUserView:createUser:nxl_user:nxw_passwordMatcher_secondPassword")
    WebElement secondPasswordInput;

    @FindBy(id = "createUserView:createUser:button_save")
    WebElement createButton;

    @Required
    @FindBy(xpath = "//div[@class=\"tabsContent\"]//input[@value=\"Cancel\"]")
    WebElement cancelButton;

    public UserCreationFormPage(WebDriver driver) {
        super(driver);
    }

    public UsersGroupsBasePage createUser(String username, String firstname, String lastname, String company,
            String email, String password, String group) throws NoSuchElementException {
        return createUser(username, firstname, lastname, company, email, password, group, false);
    }

    public UsersGroupsBasePage inviteUser(String username, String firstname, String lastname, String company,
            String email, String group) throws NoSuchElementException {
        return createUser(username, firstname, lastname, company, email, "", group, true);
    }

    private boolean isObjectChecked(int index) {
        assert (index < 2 && index >= 0);
        org.junit.Assert.assertNotNull(immediateCreation);
        org.junit.Assert.assertEquals(2, immediateCreation.size());

        return immediateCreation.get(index).isSelected();
    }

    public boolean isImmediateCreationYesSelected() {
        return isObjectChecked(1);
    }

    public UsersGroupsBasePage createUser(String username, String firstname, String lastname, String company,
            String email, String password, String group, final boolean invite) throws NoSuchElementException {
        if (!invite) {
            switchCreationFormPage();
            usernameInput.sendKeys(username);
            firstnameInput.sendKeys(firstname);
            lastnameInput.sendKeys(lastname);
            companyInput.sendKeys(company);
            emailInput.sendKeys(email);
            firstPasswordInput.sendKeys(password);
            secondPasswordInput.sendKeys(password);
            if (StringUtils.isNotBlank(group)) {
                Select2WidgetElement groups = new Select2WidgetElement(
                        driver,
                        driver.findElement(By.xpath("//div[@id='s2id_createUserView:createUser:nxl_user:nxw_groups_select2']")),
                        true);
                groups.selectValue(group);
            }
            AjaxRequestManager arm = new AjaxRequestManager(driver);
            arm.begin();
            createButton.click();
            arm.end();
        } else {
            usernameInput.sendKeys(username);
            firstnameInput.sendKeys(firstname);
            lastnameInput.sendKeys(lastname);
            companyInput.sendKeys(company);
            emailInput.sendKeys(email);
            if (StringUtils.isNotBlank(group)) {
                Select2WidgetElement groups = new Select2WidgetElement(
                        driver,
                        driver.findElement(By.xpath("//div[@id='s2id_createUserView:createUser:nxl_user:nxw_groups_select2']")),
                        true);
                groups.selectValue(group);
            }
            AjaxRequestManager arm = new AjaxRequestManager(driver);
            arm.begin();
            createButton.click();
            arm.end();
        }
        return asPage(UsersGroupsBasePage.class);
    }

    public UsersTabSubPage cancelCreation() {
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        cancelButton.click();
        arm.end();
        return asPage(UsersTabSubPage.class);
    }

    protected void switchCreationFormPage() {
        if (!isImmediateCreationYesSelected()) {
            immediateCreation.get(1).click();
            Locator.waitUntilElementPresent(By.id("createUserView:createUser:nxl_user:nxw_passwordMatcher_firstPassword"));
        }
    }
}
