/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests.fragment;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.functionaltests.Locator;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;

import com.google.common.base.Function;

/**
 * Fragment representing the gadgets container.
 *
 * @since 5.7.3
 */
public class GadgetsContainerFragment extends WebFragmentImpl {

    private static final Log log = LogFactory.getLog(GadgetsContainerFragment.class);

    public static final String GADGETS_CONTAINER_ID = "gwtContainerDiv";

    public GadgetsContainerFragment(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    public WebElement waitForGadgetsLoad() {
        return waitForGadgetsLoad("nxDocumentListData,content");
    }

    public WebElement waitForGadgetsLoad(final String mandatoryElements) {
        FluentWait<WebDriver> wait = Locator.getFluentWait();
        wait.ignoring(NoSuchElementException.class);
        Function<WebDriver, WebElement> function = new Function<WebDriver, WebElement>() {
            @Override
            public WebElement apply(WebDriver driver) {
                WebElement container = getElement();
                // iterate through all frames, and ensure opensocial ones are
                // loaded, and expect at least one opensocial frame
                boolean oneFound = false;
                List<WebElement> framesList = driver.findElements(By.xpath("//iframe"));
                if (framesList != null && !framesList.isEmpty()) {
                    List<String> mandatory = Arrays.asList(mandatoryElements.split(","));
                    for (WebElement frame : framesList) {
                        String frameName = frame.getAttribute("name");
                        if (frameName == null || !frameName.startsWith("open-social")) {
                            continue;
                        }
                        log.debug(String.format("Found one GWT gadget frame named '%s' ", frameName));
                        oneFound = true;
                        boolean loaded = false;
                        driver.switchTo().defaultContent();
                        driver.switchTo().frame(frame);
                        for (String mand : mandatory) {
                            try {
                                driver.findElement(By.id(mand));
                                loaded = true;
                                log.debug(String.format("Gadget frame '%s' mandatory element '%s' loaded", frameName,
                                        mand));
                            } catch (NoSuchElementException e) {
                                loaded = false;
                                log.debug(String.format(
                                        "Gadget frame '%s' not loaded yet, " + "mandatory element '%s' not found",
                                        frameName, mand));
                                break;
                            }
                        }
                        if (!loaded) {
                            log.debug(String.format("Gadget frame '%s' not loaded yet", frameName));
                            driver.switchTo().defaultContent();
                            return null;
                        }
                        log.debug(String.format("Gadget frame '%s' loaded", frameName));
                        driver.switchTo().defaultContent();
                    }
                }
                if (oneFound) {
                    return container;
                }
                log.debug("No gadget frame loaded yet");
                return null;
            }
        };
        return wait.until(function);
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

    public boolean isTaskGadgetEmpty(String gadgetTitle) {
        WebDriver driver = switchToFrame(gadgetTitle);
        boolean res = driver.findElement(By.id("nxDocumentListData")).getText().contains(
                "Your dashboard is empty. There are no tasks that require your intervention.");
        // switch back to parent page after that
        driver.switchTo().defaultContent();
        return res;
    }

    public WebDriver switchToFrame(String gadgetTitle) {
        WebElement title = getGadgetTitleElement(gadgetTitle);
        WebElement parent = title.findElement(By.xpath("parent::*"));
        driver.switchTo().defaultContent();
        return driver.switchTo().frame("open-social-" + parent.getAttribute("id"));
    }

}
