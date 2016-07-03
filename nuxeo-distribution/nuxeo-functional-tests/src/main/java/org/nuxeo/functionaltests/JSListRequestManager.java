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
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;

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
        Locator.waitUntilGivenFunctionIgnoring(function, NoSuchElementException.class);
    }

}
