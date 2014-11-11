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

package org.nuxeo.functionaltests.dam;

import java.util.concurrent.TimeUnit;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.fragment.WebFragmentImpl;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import com.google.common.base.Function;

/**
 * @since 5.7.3
 */
public class FoldableBoxFragment extends WebFragmentImpl {

    protected boolean isAjax;

    public FoldableBoxFragment(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    public void setAjax(boolean ajax) {
        isAjax = ajax;
    }

    public boolean isAjax() {
        return isAjax;
    }

    public void open() {
        try {
            element.findElement(
                    By.xpath("//div[@id='"
                            + id
                            + "']//h3[contains(@class, 'folded') and not(contains(@class, 'unfolded'))]/a[@class='foldableHeaderLink']")).click();
        } catch (NoSuchElementException e) {
            // do nothing
        }
    }

    public void close() {
        try {
            element.findElement(
                    By.xpath("//div[@id='"
                            + id
                            + "']//h3[contains(@class, 'unfolded')]/a[@class='foldableHeaderLink']")).click();
        } catch (NoSuchElementException e) {
            // do nothing
        }
    }

    public void edit() {
        element.findElement(By.linkText("Edit")).click();
        AbstractTest.waitUntilElementPresent(By.xpath("//h3/a[text()='Cancel']"));
        refreshElement();
        waitUntilElementPresent(By.className("buttonsGadget"));
    }

    public void save() {
        element.findElement(
                By.xpath("//p[@class='buttonsGadget']/input[@value='Save']")).click();
        if (isAjax) {
            AbstractTest.waitUntilElementPresent(By.xpath("//div[@id='" + id
                    + "']//a[text()='Edit']"));
            refreshElement();
        }
    }

    protected void refreshElement() {
        element = driver.findElement(By.id(id));
    }

    public WebElement waitUntilElementPresent(final By locator) {
        Wait<WebDriver> wait = new FluentWait<>(driver).withTimeout(
                AbstractTest.LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS).pollingEvery(
                AbstractTest.POLLING_FREQUENCY_SECONDS, TimeUnit.SECONDS).ignoring(
                NoSuchElementException.class);
        return wait.until(new Function<WebDriver, WebElement>() {
            public WebElement apply(WebDriver driver) {
                return element.findElement(locator);
            }
        });
    }

}
