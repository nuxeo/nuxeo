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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Wraps a {@link org.openqa.selenium.WebElement} and delegates all method
 * calls to the underlying {@link org.openqa.selenium.WebElement}.
 *
 * @since 5.7.3
 */
public class WebFragmentImpl implements WebFragment {

    protected final WebDriver driver;

    protected String id;

    protected WebElement element;

    public WebFragmentImpl(WebDriver driver, WebElement element) {
        this.driver = driver;
        this.element = element;
        this.id = element.getAttribute("id");
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public WebElement getElement() {
        return element;
    }

    @Override
    public void setElement(WebElement element) {
        this.element = element;
        this.id = element.getAttribute("id");
    }

    @Override
    public void click() {
        element.click();
    }

    @Override
    public void submit() {
        element.submit();
    }

    @Override
    public void sendKeys(CharSequence... keysToSend) {
        element.sendKeys(keysToSend);
    }

    @Override
    public void clear() {
        element.clear();
    }

    @Override
    public String getTagName() {
        return element.getTagName();
    }

    @Override
    public String getAttribute(String name) {
        return element.getAttribute(name);
    }

    @Override
    public boolean isSelected() {
        return element.isSelected();
    }

    @Override
    public boolean isEnabled() {
        return element.isEnabled();
    }

    @Override
    public String getText() {
        return element.getText();
    }

    @Override
    public List<WebElement> findElements(By by) {
        return element.findElements(by);
    }

    @Override
    public WebElement findElement(By by) {
        return element.findElement(by);
    }

    @Override
    public boolean isDisplayed() {
        return element.isDisplayed();
    }

    @Override
    public Point getLocation() {
        return element.getLocation();
    }

    @Override
    public Dimension getSize() {
        return element.getSize();
    }

    @Override
    public String getCssValue(String propertyName) {
        return element.getCssValue(propertyName);
    }

    @Override
    public <T extends WebFragment> T getWebFragment(By by,
            Class<T> webFragmentClass) {
        return AbstractTest.getWebFragment(by, webFragmentClass);
    }

    @Override
    public <T extends WebFragment> T getWebFragment(WebElement element,
            Class<T> webFragmentClass) {
        return AbstractTest.getWebFragment(element, webFragmentClass);
    }

    @Override
    public boolean containsText(String text) {
        return element.getText().contains(text);
    }

    @Override
    public void waitForTextToBePresent(String text) {
        Locator.waitForTextPresent(element, text);
    }

    @Override
    public void checkTextToBePresent(String text) {
        assertTrue(element.getText().contains(text));
    }

    @Override
    public void checkTextToBeNotPresent(String text) {
        assertFalse(element.getText().contains(text));
    }
}
