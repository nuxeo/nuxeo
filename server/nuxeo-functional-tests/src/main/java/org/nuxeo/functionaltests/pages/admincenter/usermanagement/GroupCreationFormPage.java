/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nelson Silva
 */
package org.nuxeo.functionaltests.pages.admincenter.usermanagement;

import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Group creation form page.
 *
 * @since 7.2
 */
public class GroupCreationFormPage extends UsersGroupsBasePage {

    @FindBy(id = "createGroupView:createGroup:nxl_group:nxw_group_name")
    WebElement nameInput;

    @FindBy(id = "createGroupView:createGroup:nxl_group:nxw_group_label")
    WebElement labelInput;

    @Required
    @FindBy(id = "s2id_createGroupView:createGroup:nxl_group:nxw_group_members_select2")
    WebElement membersSelect;

    @Required
    @FindBy(id = "s2id_createGroupView:createGroup:nxl_group:nxw_group_subgroups_select2")
    WebElement subgroupsSelect;

    @FindBy(id = "createGroupView:createGroup:button_save")
    WebElement createButton;

    @Required
    @FindBy(xpath = "//div[@class=\"tabsContent\"]//input[@value=\"Cancel\"]")
    WebElement cancelButton;

    public GroupCreationFormPage(WebDriver driver) {
        super(driver);
    }

    public UsersGroupsBasePage createGroup(String name, String label, String[] members, String[] subgroups)
            throws NoSuchElementException {
        nameInput.sendKeys(name);
        labelInput.sendKeys(label);
        if (members != null) {
            new Select2WidgetElement(driver, membersSelect, true).selectValues(members);
        }
        if (subgroups != null) {
            new Select2WidgetElement(driver, subgroupsSelect, true).selectValues(subgroups);
        }
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        Locator.waitUntilEnabledAndClick(createButton);
        arm.end();
        return asPage(UsersGroupsBasePage.class);
    }

    public GroupsTabSubPage cancelCreation() {
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        Locator.waitUntilEnabledAndClick(cancelButton);
        arm.end();
        return asPage(GroupsTabSubPage.class);
    }
}
