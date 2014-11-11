/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Mariana Cedica
 */
package org.nuxeo.functionaltests.pages;

import java.util.List;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.tabs.SummaryTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 5.7
 *
 */
public class UserHomePage extends AbstractPage {

    @Required
    @FindBy(linkText = "Dashboard")
    public WebElement dashboardLink;

    public UserHomePage(WebDriver driver) {
        super(driver);
    }

    public WebElement getGadgetsContainer() {
        try {
            // force sleep as findElementAndWaitUntilEnabled fails randomly
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }
        return findElementAndWaitUntilEnabled(By.id("gwtContainerDiv"), 5000,
                5000);
    }

    public boolean myTasksGadgetLoaded() {
        boolean visible = false;
        WebElement gtwContainer = getGadgetsContainer();
        List<WebElement> gadgets = gtwContainer.findElements(By.className("dragdrop-draggable"));
        for (WebElement gadget : gadgets) {
            WebElement title = gadget.findElement(By.className("header"));
            if (title.getText().contains("My Tasks")) {
                visible = true;
            }
        }
        return visible;
    }

    public SummaryTabSubPage redirectToTask(String taskTitle) {
        boolean visible = false;
        WebElement gtwContainer = getGadgetsContainer();
        List<WebElement> gadgets = gtwContainer.findElements(By.className("dragdrop-draggable"));
        for (WebElement gadget : gadgets) {
            WebElement title = gadget.findElement(By.className("header"));
            if (title.getText().contains("My Tasks")) {
                visible = true;
            }
            if (visible) {
                WebElement parent = title.findElement(By.xpath("parent::*"));
                WebDriver driver = loadIFrame("open-social-"
                        + parent.getAttribute("id"));
                driver.findElement(By.linkText(taskTitle)).click();
                return new SummaryTabSubPage(driver);
            }
        }
        throw new NoSuchElementException(taskTitle);
    }

    public boolean taskGadgetEmpty() {
        boolean visible = false;
        WebElement gtwContainer = getGadgetsContainer();
        List<WebElement> gadgets = gtwContainer.findElements(By.className("dragdrop-draggable"));
        for (WebElement gadget : gadgets) {
            WebElement title = gadget.findElement(By.className("header"));
            if (title.getText().contains("My Tasks")) {
                visible = true;
            }
            if (visible) {
                WebElement parent = title.findElement(By.xpath("parent::*"));
                WebDriver driver = loadIFrame("open-social-"
                        + parent.getAttribute("id"));
                return driver.findElement(By.id("nxDocumentListData")).getText().contains(
                        "Your dashboard is empty. There are no tasks that require your intervention.");
            }
        }
        return false;
    }

}
