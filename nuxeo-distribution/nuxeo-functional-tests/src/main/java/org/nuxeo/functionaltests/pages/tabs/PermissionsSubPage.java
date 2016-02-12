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
    @FindBy(xpath = "//div[contains(@class, 'jsLocalPermissions')]/*/paper-button")
    WebElement newPermission;

    @FindBy(xpath = "//paper-button[@id='block']")
    WebElement blockPermissions;

    @FindBy(xpath = "//paper-button[@id='unblock']")
    WebElement unblockPermissions;

    public PermissionsSubPage(WebDriver driver) {
        super(driver);
    }

    public boolean hasPermissionForUser(String permission, String username) {
        List<WebElement> elements = driver.findElements(By.xpath("//div[contains(@class, 'acl-table-row effective')]"));
        boolean hasPermission = false;
        for (WebElement element : elements) {
            List<WebElement> names = element.findElements(By.xpath(".//span[contains(@class, 'tag user')]"));
            if (names.isEmpty()) {
                names = element.findElements(By.xpath(".//span[contains(@class, 'tag group')]"));
            }
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

    public PermissionsSubPage blockPermissions() {
        blockPermissions.click();
        Locator.waitUntilElementPresent(By.xpath("//paper-button[@id='unblock']"));
        return asPage(PermissionsSubPage.class);
    }

    public PermissionsSubPage unblockPermissions() {
        unblockPermissions.click();
        Locator.waitUntilElementPresent(By.xpath("//paper-button[@id='block']"));
        return asPage(PermissionsSubPage.class);
    }

    public PermissionsSubPage deletePermission(String permission, String username) {
        WebElement deleteButton = findDeleteButton(permission, username);
        if (deleteButton != null) {
            deleteButton.click();
            Locator.waitUntilElementPresent(By.xpath("//h2[contains(text(), 'The following permission will be deleted')]"));
            driver.findElement(By.xpath("//paper-button[text()='Delete']")).click();
            Locator.waitUntilElementPresent(By.xpath("//span[text()='Permission deleted.']"));
        }
        return asPage(PermissionsSubPage.class);
    }

    private WebElement findDeleteButton(String permission, String username) {
        List<WebElement> elements = driver.findElements(By.xpath("//div[contains(@class, 'acl-table-row effective')]"));
        for (WebElement element : elements) {
            List<WebElement> names = element.findElements(By.xpath(".//span[contains(@class, 'tag user')]"));
            if (names.isEmpty()) {
                names = element.findElements(By.xpath(".//span[contains(@class, 'tag group')]"));
            }
            List<WebElement> perms = element.findElements(By.className("label"));
            if (names.size() > 0 && perms.size() > 0) {
                String title = names.get(0).getAttribute("title");
                String perm = perms.get(0).getText();
                if (title.startsWith(username) && permission.equalsIgnoreCase(perm)) {
                    return element.findElement(By.xpath(".//paper-icon-button[@icon='delete']"));
                }
            }
        }
        return null;
    }
}
