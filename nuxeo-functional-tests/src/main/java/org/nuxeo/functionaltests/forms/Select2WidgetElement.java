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
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.fragment.WebFragmentImpl;
import org.nuxeo.functionaltests.pages.search.SearchPage;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.google.common.base.Function;

/**
 * Convenient class to handle a select2Widget.
 *
 * @since 5.7.3
 */
public class Select2WidgetElement extends WebFragmentImpl {

    private static class Select2Wait implements Function<WebElement, Boolean> {

        public Boolean apply(WebElement element) {
            boolean result = !element.getAttribute("class").contains(
                    S2_CSS_ACTIVE_CLASS);
            return result;
        }
    }

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

    /**
     * Constructor.
     *
     * @param driver the driver
     * @param by the by locator of the widget
     */
    public Select2WidgetElement(WebDriver driver, WebElement element) {
        super(driver, element);
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

    public List<WebElement> getSuggestedEntries() {
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
        boolean found =false;
        for (WebElement el : getSelectedValues()) {
            if (el.getText().equals(displayedText)) {
                el.findElement(
                        By.xpath("a[@class='select2-search-choice-close']")).click();
                found = true;
            }
        }
        if (found) {
            Locator.waitUntilGivenFunction(new Function<WebDriver, Boolean>() {
                public Boolean apply(WebDriver driver) {
                    return !submittedValueBefore.equals(getSubmittedValue());
                }
            });
        } else {
            throw new ElementNotFoundException("remove link for select2 '" + displayedText + "' item", "", "");
        }
    }

    /**
     * Select a single value.
     *
     * @param value the value to be selected
     *
     * @since 5.7.3
     */
    public void selectValue(final String value) {
        clickOnSelect2Field();

        WebElement suggestInput = getSuggestInput();

        char c;
        for (int i = 0; i < value.length(); i++) {
            c = value.charAt(i);
            suggestInput.sendKeys(c + "");
        }

        waitSelect2();

        if (getSuggestedEntries() != null && getSuggestedEntries().size() > 1) {
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

    /**
     * Type a value in the select2 and return the suggested entries.
     *
     * @param value The value to type in the select2.
     * @return The suggested values for the parameter.
     *
     * @since 5.9.6
     */
    public List<WebElement> typeAndGetResult(final String value) {

        clickOnSelect2Field();

        WebElement suggestInput = getSuggestInput();

        suggestInput.sendKeys(value);
        try {
            waitSelect2();
        } catch (TimeoutException e) {
            log.warn("Suggestion timed out with input : " + value
                    + ". Let's try with next letter.");
        }

        return getSuggestedEntries();
    }

    /**
     * Click on the select2 field.
     *
     * @since 5.9.6
     */
    private void clickOnSelect2Field() {
        WebElement select2Field = null;
        if (mutliple) {
            select2Field = element;
        } else {
            select2Field = element.findElement(By.xpath("a[contains(@class,'select2-choice')]"));
        }
        select2Field.click();
    }

    /**
     * @return The suggest input element.
     *
     * @since 5.9.6
     */
    private WebElement getSuggestInput() {
        WebElement suggestInput = null;
        if (mutliple) {
            suggestInput = element.findElement(By.xpath("ul/li[@class='select2-search-field']/input"));
        } else {
            suggestInput = driver.findElement(By.xpath(S2_SINGLE_INPUT_XPATH));
        }

        return suggestInput;
    }

    /**
     * Do a wait on the select2 field.
     *
     * @throws TimeoutException
     *
     * @since 5.9.6
     */
    private void waitSelect2()
            throws TimeoutException {
        Wait<WebElement> wait = new FluentWait<WebElement>(
                !mutliple ? driver.findElement(By.xpath(S2_SINGLE_INPUT_XPATH))
                        : element.findElement(By.xpath(S2_MULTIPLE_INPUT_XPATH))).withTimeout(
                SELECT2_LOADING_TIMEOUT,
                TimeUnit.SECONDS).pollingEvery(
                100,
                TimeUnit.MILLISECONDS).ignoring(
                NoSuchElementException.class);
        Function<WebElement, Boolean> s2WaitFunction = new Select2Wait();
        wait.until(s2WaitFunction);
    }

    /**
     * Clear the input of the select2.
     *
     * @since 5.9.6
     */
    public void clearSuggestInput() {
        WebElement suggestInput = null;
        if (mutliple) {
            suggestInput = driver.findElement(By.xpath("//ul/li[@class='select2-search-field']/input"));
        } else {
            suggestInput = driver.findElement(By.xpath(S2_SINGLE_INPUT_XPATH));
        }

        if (suggestInput != null) {
            suggestInput.clear();
        }
    }

    /**
     * Click on the select2 element.
     *
     * @since 5.9.6
     */
    public void clickSelect2Field() {
        WebElement select2Field = null;
        if (mutliple) {
            select2Field = element;
        } else {
            select2Field = element.findElement(By.xpath("a[contains(@class,'select2-choice select2-default')]"));
        }
        select2Field.click();
    }

    /**
     * Type a value in the select2 and then simulate the enter key.
     *
     * @since 5.9.6
     */
    public SearchPage typeValueAndTypeEnter(String value) {
        clickOnSelect2Field();

        WebElement suggestInput = getSuggestInput();

        suggestInput.sendKeys(value);
        try {
            waitSelect2();
        } catch (TimeoutException e) {
            log.warn("Suggestion timed out with input : " + value
                    + ". Let's try with next letter.");
        }
        suggestInput.sendKeys(Keys.RETURN);

        return AbstractTest.asPage(SearchPage.class);
    }
}
