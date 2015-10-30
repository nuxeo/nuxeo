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
 *     Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.functionaltests.pages.admincenter.activity;

import org.nuxeo.functionaltests.EventListener;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.text.SimpleDateFormat;
import java.util.Date;

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
