package org.nuxeo.functionaltests.finders;
/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN <stan@nuxeo.com>
 */


import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * @author Sun Seng David TAN <stan@nuxeo.com>
 *
 */
public class FindElementUntil {

    long timeout;

    WebDriver driver;

    By by;

    /**
     * Create a find element object with a default time out of 4 secs
     *
     * @param driver
     * @param xpath
     */
    public FindElementUntil(WebDriver driver, By by) {
        this(driver, by, 4000);
    }

    public FindElementUntil(WebDriver driver, By by, long timeout) {
        this.timeout = timeout;
        this.driver = driver;
        this.by = by;
    }

    public WebElement find() throws ElementNotFoundException {
        long starttime = System.currentTimeMillis();
        Exception lastException = null;

        while (starttime > System.currentTimeMillis() - timeout) {
            try {
                WebElement element = driver.findElement(by);
                if (element != null) {
                    return element;
                }
                Thread.sleep(100);
            } catch (Exception e) {
                lastException = e;
            }

        }
        throw new ElementNotFoundException("Couldn't find element",
                lastException);
    }
}
