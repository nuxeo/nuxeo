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

    @FindBy(id = "createUserView:createUser")
    WebElement form;

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
        assert(index < 2 && index >= 0);
        org.junit.Assert.assertNotNull(immediateCreation);
        org.junit.Assert.assertEquals(2, immediateCreation.size());

        return immediateCreation.get(index).isSelected();
    }

    public boolean isImmediateCreationYesSelected() {
        return isObjectChecked(1);
    }

    public UsersGroupsBasePage createUser(String username, String firstname, String lastname, String company,
            String email, String password, String group, final boolean invite) throws NoSuchElementException {
        return createUser(username, firstname, lastname, company, email, password, password, group, invite);
    }

    /**
     * @since 8.2
     */
    public UsersGroupsBasePage createUser(String username, String firstname, String lastname, String company,
            String email, String password1, String password2, String group, final boolean invite)
                    throws NoSuchElementException {
        if (!invite) {
            switchCreationFormPage();
            usernameInput.sendKeys(username);
            firstnameInput.sendKeys(firstname);
            lastnameInput.sendKeys(lastname);
            companyInput.sendKeys(company);
            emailInput.sendKeys(email);
            firstPasswordInput.sendKeys(password1);
            secondPasswordInput.sendKeys(password2);
            if (StringUtils.isNotBlank(group)) {
                Select2WidgetElement groups = new Select2WidgetElement(driver,
                        driver.findElement(
                                By.xpath("//div[@id='s2id_createUserView:createUser:nxl_user:nxw_groups_select2']")),
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
                Select2WidgetElement groups = new Select2WidgetElement(driver,
                        driver.findElement(
                                By.xpath("//div[@id='s2id_createUserView:createUser:nxl_user:nxw_groups_select2']")),
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
            Locator.waitUntilElementPresent(
                    By.id("createUserView:createUser:nxl_user:nxw_passwordMatcher_firstPassword"));
        }
    }

    public WebElement getForm() {
        return form;
    }

}
