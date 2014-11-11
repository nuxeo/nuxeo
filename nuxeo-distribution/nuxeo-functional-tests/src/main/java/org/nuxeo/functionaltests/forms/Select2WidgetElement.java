/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.functionaltests.forms;

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import com.google.common.base.Function;

/**
 * Convenient class to handle a select2Widget.
 *
 * @since 5.7.3
 */
public class Select2WidgetElement {

    private static final Log log = LogFactory.getLog(Select2WidgetElement.class);

    private final static String S2_SINGLE_INPUT_XPATH = "//*[@id='select2-drop']/div/input";

    private final static String S2_MULTIPLE_INPUT_XPATH = "ul/li/input";

    private static final String S2_SUGGEST_RESULT_XPATH = "//*[@id='select2-drop']//li[contains(@class,'select2-result-selectable')]/div";

    private static final String S2_CSS_ACTIVE_CLASS = "select2-active";

    /**
     * Select2 loading timeout in seconds.
     */
    private static final int SELECT2_LOADING_TIMEOUT = 5;

    private Function<WebDriver, Boolean> s2SingleWaitFunction;

    private Function<WebDriver, Boolean> s2MultipleWaitFunction;

    private final WebElement element;

    private final WebDriver driver;

    private boolean mutliple = false;

    /**
     * Constructor.
     *
     * @param driver the driver
     * @param by the by locator of the widget
     */
    public Select2WidgetElement(WebDriver driver, final By by) {
        this.driver = driver;
        this.element = driver.findElement(by);

        s2SingleWaitFunction = new Function<WebDriver, Boolean>() {
            public Boolean apply(WebDriver driver) {
                WebElement searchInput = driver.findElement(By.xpath(S2_SINGLE_INPUT_XPATH));
                return !searchInput.getAttribute("class").contains(
                        S2_CSS_ACTIVE_CLASS);
            }
        };

        s2MultipleWaitFunction = new Function<WebDriver, Boolean>() {
            public Boolean apply(WebDriver driver) {
                WebElement searchInput = element.findElement(By.xpath(S2_MULTIPLE_INPUT_XPATH));
                return !searchInput.getAttribute("class").contains(
                        S2_CSS_ACTIVE_CLASS);
            }
        };
    }

    /**
     * Constructor.
     *
     * @param driver the driver
     * @param by the by locator of the widget
     * @param multiple whether the widget can have multiple values
     */
    public Select2WidgetElement(final WebDriver driver, final By by,
            final boolean multiple) {
        this(driver, by);
        this.mutliple = multiple;
    }

    /**
     * Select a single value.
     *
     * @param value the value to be selected
     *
     * @since 5.7.3
     */
    public void selectValue(final String value) {
        WebElement select2Field = null;
        if (mutliple) {
            select2Field = element;
        } else {
            select2Field = element.findElement(By.xpath("a[contains(@class,'select2-choice select2-default')]"));
        }
        select2Field.click();

        Wait<WebDriver> wait = new FluentWait<WebDriver>(driver).withTimeout(
                SELECT2_LOADING_TIMEOUT, TimeUnit.SECONDS).pollingEvery(100,
                TimeUnit.MILLISECONDS).ignoring(NoSuchElementException.class);

        WebElement suggestInput = null;
        if (mutliple) {
            suggestInput = driver.findElement(By.xpath("//ul/li[@class='select2-search-field']/input"));
        } else {
            suggestInput = driver.findElement(By.xpath(S2_SINGLE_INPUT_XPATH));
        }

        char c;
        for (int i = 0; i <  value.length(); i++) {
            c = value.charAt(i);
            suggestInput.sendKeys(c + "");
            try {
                wait.until(mutliple ? s2MultipleWaitFunction
                        : s2SingleWaitFunction);
            } catch (TimeoutException e) {
                if (i == (value.length() - 1)) {
                    log.error("Suggestion definitly timed out with last letter : " + c + ". There is something wrong with select2");
                    throw e;
                }
                log.warn("Suggestion timed out with letter : " + c + ". Let's try with next letter.");
            }
        }

        WebElement suggestion = driver.findElement(By.xpath(S2_SUGGEST_RESULT_XPATH));
        suggestion.click();
    }

    /**
     * Select multiple values.
     *
     * @param values the values
     *
     * @since 5.7.3
     */
    public void selectValues(final String[] values) {
        for (String value : values) {
            selectValue(value);
        }
    }

}
