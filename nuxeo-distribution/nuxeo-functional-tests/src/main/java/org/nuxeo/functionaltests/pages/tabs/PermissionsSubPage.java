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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.pagefactory.ByChained;

/**
 * @since 7.10
 */
public class PermissionsSubPage extends AbstractPage {

    private static final Log log = LogFactory.getLog(PermissionsSubPage.class);

    @Required
    @FindBy(xpath = "//paper-button[text()='New Permission']")
    WebElement newPermission;

    public PermissionsSubPage(WebDriver driver) {
        super(driver);
    }

    public boolean hasPermissionForUser(String permission, String username) {
        List<WebElement> elements = driver.findElements(By.className("acl-table-row"));
        boolean hasPermission = false;
        for (WebElement element : elements) {
            List<WebElement> divs = element.findElements(new ByChained(By.tagName("div")));
            if (divs.size() > 3) {
                String aceUsername = divs.get(0).getText();
                String aceRight = divs.get(1).getText();
                if (username.equals(aceUsername) && permission.equalsIgnoreCase(aceRight)) {
                    hasPermission = true;
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
        waitForPermissionAdded();

        return asPage(PermissionsSubPage.class);
    }

    protected void waitForPermissionAdded() {
        Locator.waitUntilGivenFunction(input -> {
            try {
                WebElement element = driver.findElement(By.xpath("//h2[text()='Add a Permission']"));
                return !element.isDisplayed();
            } catch (NoSuchElementException e) {
                return true;
            }
        });
    }
}
