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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */
package org.nuxeo.functionaltests.pages.search.aggregates;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * @since 6.0
 */
public class CheckBoxAggregateElements implements AggregateElement {

    protected WebElement element;

    protected WebDriver driver;

    public CheckBoxAggregateElements(final WebElement element) {
        super();
        this.element = element;
    }

    public CheckBoxAggregateElements(WebDriver driver, WebElement element) {
        this(element);
        this.driver = driver;
    }

    @Override
    public Map<String, Integer> getAggregates() {
        Map<String, Integer> result = new HashMap<>();
        for (WebElement e : element.findElements(By.xpath("tbody/tr/td"))) {
            String label;
            Integer count;
            String temp = e.findElement(By.xpath("label")).getText();
            Pattern regex = Pattern.compile(AGG_REGEX);
            Matcher regexMatcher = regex.matcher(temp);
            regexMatcher.find();
            label = regexMatcher.group(1);
            String s = regexMatcher.group(2);
            count = Integer.parseInt(s);
            result.put(label, count);
        }
        return result;
    }

    public void selectOrUnselect(final String label) {
        for (WebElement e : element.findElements(By.xpath("tbody/tr/td"))) {
            String select;
            String temp = e.findElement(By.xpath("label")).getText();
            Pattern regex = Pattern.compile(AGG_REGEX);
            Matcher regexMatcher = regex.matcher(temp);
            regexMatcher.find();
            select = regexMatcher.group(1);
            if (label.equals(select)) {
                AjaxRequestManager a = new AjaxRequestManager(driver);
                a.watchAjaxRequests();
                WebElement input = e.findElement(By.xpath("input"));
                Locator.waitUntilEnabled(input);
                Locator.scrollToElement(input);
                input.click();
                a.waitForAjaxRequests();
                break;
            }
        }
    }

}
