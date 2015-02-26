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
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.junit.Assert.assertEquals;

/**
 * View group details
 *
 * @since 7.2
 */
public class GroupViewTabSubPage extends UsersGroupsBasePage {

    @Required
    @FindBy(linkText = "View")
    WebElement viewGroupTab;

    @FindBy(linkText = "Delete")
    WebElement deleteGroupLink;

    @FindBy(linkText = "Edit")
    WebElement editLink;

    public GroupViewTabSubPage(WebDriver driver) {
        super(driver);
    }

    public GroupsTabSubPage deleteGroup() {
        deleteGroupLink.click();
        Alert alert = driver.switchTo().alert();
        assertEquals("Delete group?", alert.getText());
        alert.accept();
        return asPage(GroupsTabSubPage.class);
    }

    public GroupEditFormPage getEditGroupTab() {
        editLink.click();
        return asPage(GroupEditFormPage.class);
    }

    public GroupsTabSubPage backToTheList() {
        findElementWaitUntilEnabledAndClick(By.linkText("Back to the list"));
        return asPage(GroupsTabSubPage.class);
    }
}
