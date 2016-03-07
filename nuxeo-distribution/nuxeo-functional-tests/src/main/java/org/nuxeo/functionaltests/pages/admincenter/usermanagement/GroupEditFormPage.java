/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.List;
import java.util.stream.Collectors;

import org.nuxeo.functionaltests.AjaxRequestManager;
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
    @FindBy(id = "viewGroupView:editGroup:nxl_group:nxw_group_label")
    WebElement labelInput;

    @Required
    @FindBy(id = "s2id_viewGroupView:editGroup:nxl_group:nxw_group_members_select2")
    WebElement membersSelect;

    @Required
    @FindBy(id = "s2id_viewGroupView:editGroup:nxl_group:nxw_group_subgroups_select2")
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

    /**
     * @since 8.2
     */
    public List<String> getMembers() {
        return new Select2WidgetElement(driver, membersSelect, true).getSelectedValues()
                                                                    .stream()
                                                                    .map(WebElement::getText)
                                                                    .collect(Collectors.toList());
    }

    public GroupEditFormPage setMembers(String... members) {
        new Select2WidgetElement(driver, membersSelect, true).selectValues(members);
        return asPage(GroupEditFormPage.class);
    }

    /**
     * @since 8.2
     */
    public GroupEditFormPage addMember(String member) {
        new Select2WidgetElement(driver, membersSelect, true).selectValue(member);
        return asPage(GroupEditFormPage.class);
    }

    /**
     * @since 8.2
     */
    public List<String> getSubGroups() {
        return new Select2WidgetElement(driver, subgroupsSelect, true).getSelectedValues()
                                                                      .stream()
                                                                      .map(WebElement::getText)
                                                                      .collect(Collectors.toList());
    }

    public GroupEditFormPage setSubGroups(String... subgroups) {
        new Select2WidgetElement(driver, subgroupsSelect, true).selectValues(subgroups);
        return asPage(GroupEditFormPage.class);
    }

    /**
     * @since 8.2
     */
    public GroupViewTabSubPage save() {
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        saveButton.click();
        arm.end();
        return asPage(GroupViewTabSubPage.class);
    }

}
