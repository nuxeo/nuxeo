/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.functionaltests.pages.wizard;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.functionaltests.Locator;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Wait;

import com.google.common.base.Function;

/**
 * Wizard and Connect use frames and callback pages to communicate. So focusing the right frame can be tricky because,
 * for example we never want to do any test on the callback pages.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class IFrameHelper {

    protected static final Log log = LogFactory.getLog(IFrameHelper.class);

    public static final String CONNECT_IFRAME_URL_PATTERN = "/register/";

    public static final String CALLBACK_URL_PATTERN = "ConnectCallback";

    public static final String CONNECT_FRAME_NAME = "connectForm";

    private static void switchToIFrame(final WebDriver driver, final WebElement iframe) {
        Wait<WebDriver> wait = Locator.getFluentWait().ignoring(NoSuchFrameException.class,
                StaleElementReferenceException.class);
        wait.until(new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                if (iframe == null) {
                    return driver.switchTo().defaultContent() != null;
                } else {
                    return driver.switchTo().frame(iframe) != null;
                }
            };
        });
    }

    public static boolean focusOnConnectFrame(WebDriver driver) {
        if (!driver.getCurrentUrl().contains(CONNECT_IFRAME_URL_PATTERN)) {
            try {
                WebElement connectFormIFrame = Locator.findElementWithTimeout(By.id(CONNECT_FRAME_NAME));
                switchToIFrame(driver, connectFormIFrame);
            } catch (TimeoutException e) {
                log.error("Unable to find IFrame on page " + driver.getCurrentUrl());
                return false;
            }
            return true;
        }
        return false;
    }

    public static boolean focusOnWizardPage(WebDriver driver) {

        Locator.waitUntilURLNotContain(CALLBACK_URL_PATTERN);

        // if we're coming from an iframe, driver.getCurrentUrl() can be empty
        // switch back to main frame without testing the URL
        try {
            switchToIFrame(driver, null);
            return true;
        } catch (TimeoutException e) {
            log.error("Unable to find top windows on page " + driver.getCurrentUrl());
            return false;
        }
    }
}
