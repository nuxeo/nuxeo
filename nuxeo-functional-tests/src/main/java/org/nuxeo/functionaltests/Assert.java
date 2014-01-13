/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
     * Returns true if {@code text} is present in the element retrieved with the
     * given method.
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
