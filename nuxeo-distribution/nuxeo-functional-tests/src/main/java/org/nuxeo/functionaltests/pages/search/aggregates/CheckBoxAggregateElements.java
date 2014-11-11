/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.functionaltests.pages.search.aggregates;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.functionaltests.AjaxRequestManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * @since 5.9.6
 */
public class CheckBoxAggregateElements {

    public static final String AGG_REGEX = "(.*) \\((.*)\\)";

    protected WebElement element;

    protected WebDriver driver;

    public CheckBoxAggregateElements(final WebElement element) {
        super();
        this.element = element;
    }

    /**
     * @param driver
     * @param coverageAggregate
     */
    public CheckBoxAggregateElements(WebDriver driver, WebElement element) {
        this(element);
        this.driver = driver;
    }

    public Map<String, Integer> getAggregates() {
        Map<String, Integer> result = new HashMap<String, Integer>();
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
                input.click();
                a.waitForAjaxRequests();
                break;
            }
        }
    }

}
