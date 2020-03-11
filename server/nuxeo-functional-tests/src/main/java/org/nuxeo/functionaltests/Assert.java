/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.functionaltests;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

/**
 * Helper class providing assert methods on WebElement conditions.
 *
 * @since 5.9.2
 */
public class Assert {

    /**
     * Returns true if corresponding element is found in the test page.
     *
     * @since 5.7
     */
    public static boolean hasElement(By by) {
        boolean present;
        try {
            AbstractTest.driver.findElement(by);
            present = true;
        } catch (NoSuchElementException e) {
            present = false;
        }
        return present;
    }

    /**
     * Returns true if corresponding element is found under web element.
     *
     * @since 8.4
     */
    public static boolean hasChild(WebElement webElement, By by) {
        try {
            webElement.findElement(by);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Returns true if {@code text} is present in the element retrieved with the given method.
     *
     * @since 5.7.3
     */
    public static boolean isTextPresent(By by, String text) {
        return isTextPresent(AbstractTest.driver.findElement(by), text);
    }

    /**
     * Returns true if {@code text} is present in the given element.
     *
     * @since 5.7.3
     */
    public static boolean isTextPresent(WebElement element, String text) {
        return element.getText().contains(text);
    }

}
