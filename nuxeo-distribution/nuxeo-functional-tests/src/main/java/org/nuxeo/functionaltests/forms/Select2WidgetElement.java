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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.functionaltests.forms;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.fragment.WebFragmentImpl;
import org.nuxeo.functionaltests.pages.search.SearchPage;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
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

        @Override
        public Boolean apply(WebElement element) {
            boolean result = !element.getAttribute("class").contains(S2_CSS_ACTIVE_CLASS);
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

    protected boolean multiple = false;

    /**
     * Constructor.
     *
     * @param driver the driver
     * @param id the id of the widget
     * @since 7.1
     */
    public Select2WidgetElement(WebDriver driver, String id) {
        this(driver, driver.findElement(By.id(id)));
    }

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
    public Select2WidgetElement(final WebDriver driver, WebElement element, final boolean multiple) {
        this(driver, element);
        this.multiple = multiple;
    }

    /**
     * @since 5.9.3
     */
    public WebElement getSelectedValue() {
        if (multiple) {
            throw new UnsupportedOperationException("The select2 is multiple and has multiple selected values");
        }
        return element.findElement(By.xpath(S2_SINGLE_CURRENT_SELECTION_XPATH));
    }

    /**
     * @since 5.9.3
     */
    public List<WebElement> getSelectedValues() {
        if (!multiple) {
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
        String submittedEltId = element.getAttribute("id").substring("s2id_".length(), eltId.length());
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
     * @since 8.1
     */
    public void removeSelection() {
        if (multiple) {
            throw new UnsupportedOperationException("The select2 is multiple, use #removeSelection(value) instead");
        }
        element.findElement(By.className("select2-search-choice-close")).click();
    }

    /**
     * @since 5.9.3
     */
    public void removeFromSelection(final String displayedText) {
        if (!multiple) {
            throw new UnsupportedOperationException("The select2 is not multiple, use #removeSelection instead");
        }
        final String submittedValueBefore = getSubmittedValue();
        boolean found = false;
        for (WebElement el : getSelectedValues()) {
            if (el.getText().equals(displayedText)) {
                el.findElement(By.xpath("a[@class='select2-search-choice-close']")).click();
                found = true;
            }
        }
        if (found) {
            Locator.waitUntilGivenFunction(new Function<WebDriver, Boolean>() {
                @Override
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
     * @since 5.7.3
     */
    public void selectValue(final String value) {
        selectValue(value, false, false);
    }

    /**
     * @since 7.1
     */
    public void selectValue(final String value, final boolean wait4A4J) {
        selectValue(value, wait4A4J, false);
    }

    /**
     * Select given value, waiting for JSF ajax requests or not, and typing the whole suggestion or not (use false speed
     * up selection when exactly one suggestion is found, use true when creating new suggestions).
     *
     * @param value string typed in the suggest box
     * @param wait4A4J use true if request is triggering JSF ajax calls
     * @param typeAll use false speed up selection when exactly one suggestion is found, use true when creating new
     *            suggestions.
     * @since 7.10
     */
    public void selectValue(final String value, final boolean wait4A4J, final boolean typeAll) {
        clickSelect2Field();

        WebElement suggestInput = getSuggestInput();

        int nbSuggested = Integer.MAX_VALUE;
        char c;
        for (int i = 0; i < value.length(); i++) {
            c = value.charAt(i);
            suggestInput.sendKeys(c + "");
            waitSelect2();
            if (i >= 2) {
                if (getSuggestedEntries().size() > nbSuggested) {
                    throw new IllegalArgumentException(
                            "More suggestions than expected for " + element.getAttribute("id"));
                }
                nbSuggested = getSuggestedEntries().size();
                if (!typeAll && nbSuggested == 1) {
                    break;
                }
            }
        }

        waitSelect2();

        List<WebElement> suggestions = getSuggestedEntries();
        if (suggestions == null || suggestions.isEmpty()) {
            log.warn("Suggestion for element " + element.getAttribute("id") + " returned no result.");
            return;
        }
        WebElement suggestion = suggestions.get(0);
        if (suggestions.size() > 1) {
            log.warn("Suggestion for element " + element.getAttribute("id")
                    + " returned more than 1 result, the first suggestion will be selected : " + suggestion.getText());
        }

        AjaxRequestManager arm = new AjaxRequestManager(driver);
        if (wait4A4J) {
            arm.watchAjaxRequests();
        }
        try {
            suggestion.click();
        } catch (StaleElementReferenceException e) {
            suggestion = driver.findElement(By.xpath(S2_SUGGEST_RESULT_XPATH));
            suggestion.click();
        }
        if (wait4A4J) {
            arm.waitForAjaxRequests();
        }
    }

    /**
     * Select multiple values.
     *
     * @param values the values
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
     * @since 6.0
     */
    public List<WebElement> typeAndGetResult(final String value) {

        clickSelect2Field();

        WebElement suggestInput = getSuggestInput();

        suggestInput.sendKeys(value);
        try {
            waitSelect2();
        } catch (TimeoutException e) {
            log.warn("Suggestion timed out with input : " + value + ". Let's try with next letter.");
        }

        return getSuggestedEntries();
    }

    /**
     * Click on the select2 field.
     *
     * @since 6.0
     */
    public void clickSelect2Field() {
        WebElement select2Field = null;
        if (multiple) {
            select2Field = element;
        } else {
            select2Field = element.findElement(By.xpath("a[contains(@class,'select2-choice')]"));
        }
        select2Field.click();
    }

    /**
     * @return The suggest input element.
     * @since 6.0
     */
    private WebElement getSuggestInput() {
        WebElement suggestInput = null;
        if (multiple) {
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
     * @since 6.0
     */
    private void waitSelect2() throws TimeoutException {
        Wait<WebElement> wait = new FluentWait<WebElement>(
                !multiple ? driver.findElement(By.xpath(S2_SINGLE_INPUT_XPATH))
                        : element.findElement(By.xpath(S2_MULTIPLE_INPUT_XPATH))).withTimeout(SELECT2_LOADING_TIMEOUT,
                                TimeUnit.SECONDS).pollingEvery(100, TimeUnit.MILLISECONDS).ignoring(
                                        NoSuchElementException.class);
        Function<WebElement, Boolean> s2WaitFunction = new Select2Wait();
        wait.until(s2WaitFunction);
    }

    /**
     * Clear the input of the select2.
     *
     * @since 6.0
     */
    public void clearSuggestInput() {
        WebElement suggestInput = null;
        if (multiple) {
            suggestInput = driver.findElement(By.xpath("//ul/li[@class='select2-search-field']/input"));
        } else {
            suggestInput = driver.findElement(By.xpath(S2_SINGLE_INPUT_XPATH));
        }

        if (suggestInput != null) {
            suggestInput.clear();
        }
    }

    /**
     * Type a value in the select2 and then simulate the enter key.
     *
     * @since 6.0
     */
    public SearchPage typeValueAndTypeEnter(String value) {
        clickSelect2Field();

        WebElement suggestInput = getSuggestInput();

        suggestInput.sendKeys(value);
        try {
            waitSelect2();
        } catch (TimeoutException e) {
            log.warn("Suggestion timed out with input : " + value + ". Let's try with next letter.");
        }
        suggestInput.sendKeys(Keys.RETURN);

        return AbstractTest.asPage(SearchPage.class);
    }

}
