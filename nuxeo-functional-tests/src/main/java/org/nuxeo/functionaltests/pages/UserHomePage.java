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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.tabs.SummaryTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import com.google.common.base.Function;

/**
 * @since 5.7
 */
public class UserHomePage extends AbstractPage {

    @Required
    @FindBy(linkText = "Dashboard")
    public WebElement dashboardLink;

    public UserHomePage(WebDriver driver) {
        super(driver);
    }

    public WebElement waitForGadgetsLoad() {
        return waitForGadgetsLoad("nxDocumentListData,content");
    }

    public WebElement waitForGadgetsLoad(final String mandatoryElements) {
        Wait<WebDriver> wait = new FluentWait<WebDriver>(driver).withTimeout(
                AbstractTest.LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS).pollingEvery(
                5, TimeUnit.SECONDS).ignoring(NoSuchElementException.class);
        return wait.until(new Function<WebDriver, WebElement>() {
            public WebElement apply(WebDriver driver) {
                WebElement container;
                try {
                    container = driver.findElement(By.id("gwtContainerDiv"));
                } catch (NoSuchElementException e) {
                    return null;
                }
                // iterate through all frames, and ensure opensocial ones are
                // loaded, and expect at least one opensocial frame
                boolean oneFound = false;
                List<WebElement> framesList = driver.findElements(By.xpath("//iframe"));
                if (framesList != null && !framesList.isEmpty()) {
                    List<String> mandatory = Arrays.asList(mandatoryElements.split(","));
                    for (WebElement frame : framesList) {
                        String frameName = frame.getAttribute("name");
                        if (frameName == null
                                || !frameName.startsWith("open-social")) {
                            continue;
                        }
                        oneFound = true;
                        boolean loaded = false;
                        driver.switchTo().defaultContent();
                        driver.switchTo().frame(frame);
                        for (String mand : mandatory) {
                            try {
                                driver.findElement(By.id(mand));
                                loaded = true;
                                break;
                            } catch (NoSuchElementException e) {
                            }
                        }
                        if (!loaded) {
                            driver.switchTo().defaultContent();
                            break;
                        }
                        driver.switchTo().defaultContent();
                    }
                }
                if (oneFound) {
                    return container;
                }
                return null;
            }
        });
    }

    public boolean isGadgetLoaded(String gadgetTitle) {
        return getGadgetTitleElement(gadgetTitle) != null;
    }

    public WebElement getGadgetTitleElement(String gadgetTitle) {
        WebElement gtwContainer = waitForGadgetsLoad();
        List<WebElement> gadgets = gtwContainer.findElements(By.className("dragdrop-draggable"));
        for (WebElement gadget : gadgets) {
            WebElement title = gadget.findElement(By.className("header"));
            if (title.getText().contains(gadgetTitle)) {
                return title;
            }
        }
        throw new NoSuchElementException(gadgetTitle);
    }

    public boolean isTaskGadgetLoaded() {
        return isGadgetLoaded("My Tasks");
    }

    public SummaryTabSubPage redirectToTask(String taskTitle) {
        WebElement title = getGadgetTitleElement("My Tasks");
        WebElement parent = title.findElement(By.xpath("parent::*"));
        WebDriver driver = switchToFrame("open-social-"
                + parent.getAttribute("id"));
        driver.findElement(By.linkText(taskTitle)).click();
        return new SummaryTabSubPage(driver);
    }

    public boolean isTaskGadgetEmpty() {
        WebElement title = getGadgetTitleElement("My Tasks");
        WebElement parent = title.findElement(By.xpath("parent::*"));
        WebDriver driver = switchToFrame("open-social-"
                + parent.getAttribute("id"));
        return driver.findElement(By.id("nxDocumentListData")).getText().contains(
                "Your dashboard is empty. There are no tasks that require your intervention.");
    }

}
