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
