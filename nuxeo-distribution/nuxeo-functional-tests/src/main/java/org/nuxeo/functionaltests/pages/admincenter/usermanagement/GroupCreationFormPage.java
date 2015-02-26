/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nelson Silva
 */
package org.nuxeo.functionaltests.pages.admincenter.usermanagement;

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
        createButton.click();
        return asPage(UsersGroupsBasePage.class);
    }

    public GroupsTabSubPage cancelCreation() {
        cancelButton.click();
        return asPage(GroupsTabSubPage.class);
    }
}
