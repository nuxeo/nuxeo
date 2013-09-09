/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.functionaltests.fragment;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;

/**
 * @since 5.7.3
 */
public interface WebFragment {

    // custom API

    WebElement getElement();

    void setElement(WebElement element);

    String getId();

    boolean containsText(String text);

    void waitForTextToBePresent(String text);

    void checkTextToBePresent(String text);

    void checkTextToBeNotPresent(String text);

    <T extends WebFragment> T getWebFragment(By by, Class<T> webFragmentClass);

    <T extends WebFragment> T getWebFragment(WebElement element,
            Class<T> webFragmentClass);

    // WebElement API

    void click();

    void submit();

    void sendKeys(CharSequence... keysToSend);

    void clear();

    String getTagName();

    String getAttribute(String name);

    boolean isSelected();

    boolean isEnabled();

    String getText();

    List<WebElement> findElements(By by);

    WebElement findElement(By by);

    boolean isDisplayed();

    Point getLocation();

    Dimension getSize();

    String getCssValue(String propertyName);

}
