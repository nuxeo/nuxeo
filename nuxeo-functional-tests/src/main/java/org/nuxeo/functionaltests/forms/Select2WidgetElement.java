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

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.ScreenshotTaker;
import org.nuxeo.functionaltests.fragment.WebFragmentImpl;
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
public class Select2WidgetElement extends WebFragmentImpl {

    private static final Log log = LogFactory.getLog(Select2WidgetElement.class);

    private static final String S2_CSS_ACTIVE_CLASS = "select2-active";

    private static final String S2_MULTIPLE_CURRENT_SELECTION_XPATH = "ul[@class='select2-choices']/li[@class='select2-search-choice']";

    private final static String S2_MULTIPLE_INPUT_XPATH = "ul/li/input";

    private static final String S2_SINGLE_CURRENT_SELECTION_XPATH = "a[@class='select2-choice']/span[@class='select2-chosen']";

    private final static String S2_SINGLE_INPUT_XPATH = "//*[@id='select2-drop']/div/input";

    private static final String S2_SUGGEST_RESULT_XPATH = "//*[@id='select2-drop']//li[contains(@class,'select2-result-selectable')]/div";

    /**
     * Select2 loading timeout in seconds.
     */
    private static final int SELECT2_LOADING_TIMEOUT = 20;

    private boolean mutliple = false;

    private Function<WebElement, Boolean> s2WaitFunction;

    /**
     * Constructor.
     *
     * @param driver the driver
     * @param by the by locator of the widget
     */
    public Select2WidgetElement(WebDriver driver, WebElement element) {
        super(driver, element);

        s2WaitFunction = new Function<WebElement, Boolean>() {
            public Boolean apply(WebElement element) {
                WebElement searchInput = element;
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
    public Select2WidgetElement(final WebDriver driver, WebElement element,
            final boolean multiple) {
        this(driver, element);
        this.mutliple = multiple;
    }

    /**
     * @since 5.9.3
     */
    public WebElement getSelectedValue() {
        if (mutliple) {
            throw new UnsupportedOperationException(
                    "The select2 is multiple and has multiple selected values");
        }
        return element.findElement(By.xpath(S2_SINGLE_CURRENT_SELECTION_XPATH));
    }

    /**
     * @since 5.9.3
     */
    public List<WebElement> getSelectedValues() {
        if (!mutliple) {
            throw new UnsupportedOperationException(
                    "The select2 is not multiple and can't have multiple selected values");
        }
        return element.findElements(By.xpath(S2_MULTIPLE_CURRENT_SELECTION_XPATH));
    }

    /**
     * @since 5.9.3
     */
    protected String getSubmittedValue() {
        String eltId = element.getAttribute("id");
        String submittedEltId = element.getAttribute("id").substring("s2id_".length(),
                eltId.length());
        return driver.findElement(By.id(submittedEltId)).getAttribute("value");
    }

    protected List<WebElement> getSuggestedEntries() {
        try {
            return driver.findElements(By.xpath(S2_SUGGEST_RESULT_XPATH));
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    /**
     * @since 5.9.3
     */
    public void removeFromSelection(final String displayedText) {
        if (!mutliple) {
            throw new UnsupportedOperationException(
                    "The select2 is not multiple and you can't remove a specific value");
        }
        final String submittedValueBefore = getSubmittedValue();
        for (WebElement el : getSelectedValues()) {
            if (el.getText().endsWith("/" + displayedText)) {
                el.findElement(
                        By.xpath("a[@class='select2-search-choice-close']")).click();
            }
        }
        Locator.waitUntilGivenFunction(new Function<WebDriver, Boolean>() {
            public Boolean apply(WebDriver driver) {
                return !submittedValueBefore.equals(getSubmittedValue());
            }
        });
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

        Wait<WebElement> wait = new FluentWait<WebElement>(
                !mutliple ? driver.findElement(By.xpath(S2_SINGLE_INPUT_XPATH))
                        : element.findElement(By.xpath(S2_MULTIPLE_INPUT_XPATH))).withTimeout(
                SELECT2_LOADING_TIMEOUT, TimeUnit.SECONDS).pollingEvery(100,
                TimeUnit.MILLISECONDS).ignoring(NoSuchElementException.class);

        WebElement suggestInput = null;
        if (mutliple) {
            suggestInput = driver.findElement(By.xpath("//ul/li[@class='select2-search-field']/input"));
        } else {
            suggestInput = driver.findElement(By.xpath(S2_SINGLE_INPUT_XPATH));
        }

        char c;
        for (int i = 0; i < value.length(); i++) {
            c = value.charAt(i);
            suggestInput.sendKeys(c + "");
            try {
                wait.until(s2WaitFunction);
            } catch (TimeoutException e) {
                if (i == (value.length() - 1)) {
                    log.error("Suggestion definitly timed out with last letter : "
                            + c + ". There is something wrong with select2");
                    throw e;
                }
                log.warn("Suggestion timed out with letter : " + c
                        + ". Let's try with next letter.");
            }
        }

        if (getSuggestedEntries() != null && getSuggestedEntries().size() > 1) {
            ScreenshotTaker screenshotTaker = new ScreenshotTaker();
            screenshotTaker.takeScreenshot(driver, "DEBUG-NXP-13875");
            log.warn("Suggestion for element "
                    + element.getAttribute("id")
                    + " returned more than 1 result, the first suggestion will be selected : "
                    + getSuggestedEntries().get(0).getText());
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
