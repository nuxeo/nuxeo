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
 * @since 7.2
 */
public class JSListRequestManager {

    protected JavascriptExecutor js;

    protected boolean active;

    protected int count;

    public JSListRequestManager(WebDriver driver) {
        super();
        js = (JavascriptExecutor) driver;
        reset();
    }

    protected void reset() {
        active = false;
        count = 0;
    }

    public void begin() {
        StringBuilder sb = new StringBuilder();
        sb.append("if (window.NuxeoTestJsList === undefined) {");
        sb.append("  window.NuxeoTestJsList = function() {");
        sb.append("    var e = {};");
        sb.append("    e.jsRequestStarted = false;");
        sb.append("    e.jsRequestFinished = false;");
        sb.append("    e.jsRequestActiveCount = 0;");
        sb.append("    e.increment = function() {");
        sb.append("      e.jsRequestStarted = true;");
        sb.append("      e.jsRequestFinished = false;");
        sb.append("      e.jsRequestActiveCount++;");
        sb.append("    };");
        sb.append("    e.decrement = function() {");
        sb.append("      e.jsRequestActiveCount--;");
        sb.append("      if (e.jsRequestActiveCount == 0) {");
        sb.append("        e.jsRequestFinished = true;");
        sb.append("      }");
        sb.append("    };");
        sb.append("    e.finished = function() {");
        sb.append("      return e.jsRequestStarted && e.jsRequestFinished;");
        sb.append("    };");
        sb.append("   return e");
        sb.append("  }();");
        sb.append("}");
        sb.append("  nuxeo.utils.addOnEvent(function(data) {");
        sb.append("    if (data.status == 'begin') {window.NuxeoTestJsList.increment();}");
        sb.append("    if (data.status == 'success') {window.NuxeoTestJsList.decrement();}");
        sb.append("  });");
        js.executeScript(sb.toString());
    }

    public void end() {
        waitUntil((new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                Boolean res = (Boolean) js.executeScript("return window.NuxeoTestJsList.finished();");
                return res;
            }
        }));
    }

    private void waitUntil(Function<WebDriver, Boolean> function) {
        Wait<WebDriver> wait = new FluentWait<WebDriver>(AbstractTest.driver).withTimeout(
                AbstractTest.LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS).pollingEvery(
                AbstractTest.POLLING_FREQUENCY_MILLISECONDS, TimeUnit.MILLISECONDS).ignoring(
                NoSuchElementException.class);
        wait.until(function);
    }

}
