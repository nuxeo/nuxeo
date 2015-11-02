/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.functionaltests.pages.search.aggregates;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public Map<String, Integer> getAggregates() {
        Map<String, Integer> result = new HashMap<String, Integer>();
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
        if (mutliple) {
            select2Field = element.findElement(By.xpath("ul/li/input"));
        } else {
            select2Field = element.findElement(By.xpath("a[contains(@class,'select2-choice')]"));
        }
        select2Field.click();
    }

}
