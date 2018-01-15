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
package org.nuxeo.functionaltests.pages.admincenter;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Wizard and Connect use frames and callback pages to communicate. So focusing the right frame can be tricky because,
 * for example we never want to do any test on the callback pages.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class IFrameHelper {

    public static final String WE_IFRAME_URL_PATTERN = "/site/connectClient";

    public static final int NBTRY = 10;

    public static boolean focusOnWEIFrame(WebDriver driver) {
        return focusOnWEIFrame(driver, NBTRY);
    }

    protected static void wait(int nbSeconds) {
        try {
            Thread.sleep(nbSeconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static boolean focusOnWEIFrame(WebDriver driver, int nbTry) {

        if (!driver.getCurrentUrl().contains(WE_IFRAME_URL_PATTERN)) {
            if (nbTry > 0) {
                try {
                    WebElement iFrame = driver.findElement(By.id("connectIframe"));
                    if (iFrame != null) {
                        driver.switchTo().frame(iFrame);
                        return true;
                    } else {
                        wait(2);
                        return focusOnWEIFrame(driver, nbTry - 1);
                    }
                } catch (Throwable e) {
                    System.out.println("Retry to find IFrame on page " + driver.getCurrentUrl());
                    wait(2);
                    return focusOnWEIFrame(driver, nbTry - 1);
                }
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

}
