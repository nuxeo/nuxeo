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

    <T extends WebFragment> T getWebFragment(WebElement element, Class<T> webFragmentClass);

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
