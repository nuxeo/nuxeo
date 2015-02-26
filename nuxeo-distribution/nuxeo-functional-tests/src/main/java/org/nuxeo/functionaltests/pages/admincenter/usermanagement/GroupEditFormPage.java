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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Edit group details
 *
 * @since 7.2
 */
public class GroupEditFormPage extends UsersGroupsBasePage {

    @Required
    @FindBy(id = "viewGroupView:editGroup:nxl_group_1:nxw_group_label_1")
    WebElement labelInput;

    @Required
    @FindBy(id = "s2id_viewGroupView:editGroup:nxl_group_1:nxw_group_members_1_select2")
    WebElement membersSelect;

    @Required
    @FindBy(id = "s2id_viewGroupView:editGroup:nxl_group_1:nxw_group_subgroups_1_select2")
    WebElement subgroupsSelect;

    @Required
    @FindBy(xpath = "//input[@value=\"Save\"]")
    WebElement saveButton;

    public GroupEditFormPage(WebDriver driver) {
        super(driver);
    }

    public void setLabel(String label) {
        labelInput.clear();
        labelInput.sendKeys(label);
    }

    public void setMembers(String... members) {
        new Select2WidgetElement(driver, membersSelect, true).selectValues(members);
    }

    public void setSubGroups(String... subgroups) {
        new Select2WidgetElement(driver, subgroupsSelect, true).selectValues(subgroups);
    }

}
