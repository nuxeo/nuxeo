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
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import com.google.common.base.Function;

/**
 * @since 5.9.6
 */
public class AjaxRequestManager {

    protected JavascriptExecutor js;

    protected boolean active;

    protected int count;

    public AjaxRequestManager(WebDriver driver) {
        super();
        this.js = (JavascriptExecutor) driver;
        reset();
    }

    protected void reset() {
        this.active = false;
        this.count = 0;
    }

    public void watchAjaxRequests() {
        StringBuilder sb = new StringBuilder();
        sb.append("if (window.ajaxListenerSet === undefined) {");
        sb.append("window.ajaxListenerSet = true;");
        sb.append("window.NuxeoTestFaces = function() {");
        sb.append("  var e = {};");
        sb.append("  e.jsf2AjaxRequestStarted = false;");
        sb.append("  e.jsf2AjaxRequestFinished = false;");
        sb.append("  e.jsf2AjaxRequestActiveCount = 0;");
        sb.append("  e.increment = function() {");
        sb.append("    e.jsf2AjaxRequestStarted = true;");
        sb.append("    e.jsf2AjaxRequestFinished = false;");
        sb.append("    e.jsf2AjaxRequestActiveCount++;");
        sb.append("  };");
        sb.append("  e.decrement = function() {");
        sb.append("    e.jsf2AjaxRequestActiveCount--;");
        sb.append("    if (e.jsf2AjaxRequestActiveCount == 0) {");
        sb.append("      e.jsf2AjaxRequestFinished = true;");
        sb.append("    }");
        sb.append("  };");
        sb.append("  e.finished = function() {");
        sb.append("    return e.jsf2AjaxRequestStarted && e.jsf2AjaxRequestFinished;");
        sb.append("  };");
        sb.append(" return e");
        sb.append("}();");
        sb.append("if (typeof jsf !== 'undefined') {");
        sb.append("  jsf.ajax.addOnEvent(function(e) {"
                + "if (e.status == 'begin') {window.NuxeoTestFaces.increment();}"
                + "if (e.status == 'success') {window.NuxeoTestFaces.decrement();}"
                + "})");
        sb.append("}");
        sb.append("}");
        js.executeScript(sb.toString());
    }

    public void waitForAjaxRequests() {
        Wait<WebDriver> wait = new FluentWait<WebDriver>(AbstractTest.driver).withTimeout(
                AbstractTest.LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS).pollingEvery(
                AbstractTest.POLLING_FREQUENCY_MILLISECONDS,
                TimeUnit.MILLISECONDS).ignoring(NoSuchElementException.class);
        wait.until((new Function<WebDriver, Boolean>() {
            public Boolean apply(WebDriver driver) {
                Boolean res = (Boolean) js.executeScript("return window.NuxeoTestFaces.finished();");
                return res;
            }
        }));
    }
}
