/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */
package org.nuxeo.functionaltests.pages.tabs;

import java.util.List;

import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 7.10
 */
public class PermissionsSubPage extends AbstractPage {

    @Required
    @FindBy(xpath = "//paper-button[text()='New Permission']")
    WebElement newPermission;

    public PermissionsSubPage(WebDriver driver) {
        super(driver);
    }

    public boolean hasPermissionForUser(String permission, String username) {
        List<WebElement> elements = driver.findElements(By.xpath("//div[contains(@class, 'acl-table-row effective')]"));
        boolean hasPermission = false;
        for (WebElement element : elements) {
            List<WebElement> names = element.findElements(By.xpath(".//span[contains(@class, 'tag user')]"));
            List<WebElement> perms = element.findElements(By.className("label"));
            if (names.size() > 0 && perms.size() > 0) {
                String title = names.get(0).getAttribute("title");
                String perm = perms.get(0).getText();
                if (title.startsWith(username) && permission.equalsIgnoreCase(perm)) {
                    hasPermission = true;
                    break;
                }
            }
        }
        return hasPermission;
    }

    public PermissionsSubPage grantPermissionForUser(String permission, String username) {

        newPermission.click();

        WebElement addPermissionH2 = findElementWithTimeout(By.xpath("//h2[text()='Add a Permission']"));
        WebElement popup = addPermissionH2.findElement(By.xpath(".."));

        Select2WidgetElement userSelection = new Select2WidgetElement(driver,
                popup.findElement(By.className("select2-container")), false);
        userSelection.selectValue(username);

        // select the permission
        popup.findElement(By.tagName("iron-icon")).click();
        Locator.waitUntilGivenFunction(input -> {
            try {
                WebElement el = popup.findElement(By.tagName("paper-item"));
                return el.isDisplayed();
            } catch (NoSuchElementException e) {
                return false;
            }
        });
        List<WebElement> elements = popup.findElements(By.tagName("paper-item"));
        for (WebElement element : elements) {
            if (permission.equalsIgnoreCase(element.getText())) {
                element.click();
                break;
            }
        }

        // click on Create
        popup.findElement(By.xpath(".//paper-button[text()='Create']")).click();
        waitForPermissionAdded(permission, username);

        return asPage(PermissionsSubPage.class);
    }

    protected void waitForPermissionAdded(String permission, String username) {
        Locator.waitUntilGivenFunction(input -> hasPermissionForUser(permission, username));
    }
}
