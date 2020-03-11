/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.functionaltests.pages.search.aggregates;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * @since 7.4
 */
public class Select2AggregateElement extends Select2WidgetElement implements AggregateElement {

    public Select2AggregateElement(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    public Select2AggregateElement(WebDriver driver, WebElement element, boolean multiple) {
        super(driver, element, multiple);
    }

    @Override
    public Map<String, Integer> getAggregates() {
        Map<String, Integer> result = new HashMap<>();
        clickSelect2Field();
        for (WebElement e : getSuggestedEntries()) {
            String label;
            Integer count;
            String temp = e.getText();
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

    @Override
    public void clickSelect2Field() {
        WebElement select2Field = null;
        if (multiple) {
            select2Field = element.findElement(By.xpath("ul/li/input"));
        } else {
            select2Field = element.findElement(By.xpath("a[contains(@class,'select2-choice')]"));
        }
        Locator.waitUntilEnabled(select2Field);
        Locator.scrollToElement(select2Field);
        select2Field.click();
    }

}
