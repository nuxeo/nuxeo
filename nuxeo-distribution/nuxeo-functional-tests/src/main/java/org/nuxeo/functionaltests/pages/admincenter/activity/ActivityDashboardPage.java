/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.functionaltests.pages.admincenter.activity;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.nuxeo.functionaltests.EventListener;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * @since 7.10
 */
public abstract class ActivityDashboardPage extends ActivityPage {

    public static final String DATA_CHANGED_EVENT = "data-changed";

    private final String selector;

    private final JavascriptExecutor js;

    public ActivityDashboardPage(WebDriver driver, String selector) {
        super(driver);
        this.js = (JavascriptExecutor) driver;
        this.selector = selector;
        // ensure our elements are ready
        findElementsWithTimeout(By.cssSelector(selector));
    }

    public EventListener listenForDataChanges() {
        return new EventListener(driver, ActivityDashboardPage.DATA_CHANGED_EVENT, selector);
    }

    public void setStartDate(Date date) {
        String fmtDate = new SimpleDateFormat("yyyy-MM-dd").format(date);
        StringBuilder sb = new StringBuilder();
        sb.append("var date = document.querySelectorAll(\"input[type='date']\")[0];");
        sb.append("date.value = '" + fmtDate + "';");
        // force set the date since change events might not be triggered
        sb.append("var els = document.querySelectorAll('" + selector +"');");
        sb.append("for (var i=0; i<els.length; i++) { els[i].startDate = '" + fmtDate + "'; }");
        js.executeScript(sb.toString());
    }

    public void setEndDate(Date date) {
        String fmtDate = new SimpleDateFormat("yyyy-MM-dd").format(date);
        StringBuilder sb = new StringBuilder();
        sb.append("var date = document.querySelectorAll(\"input[type='date']\")[1];");
        sb.append("date.value = '" + fmtDate + "';");
        // force set the date since change events might not be triggered
        sb.append("var els = document.querySelectorAll('" + selector +"');");
        sb.append("for (var i=0; i<els.length; i++) { els[i].endDate = '" + fmtDate + "'; }");
        js.executeScript(sb.toString());
    }
}
