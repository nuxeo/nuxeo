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
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;

import com.google.common.base.Function;

/**
 * @since 6.0
 */
public class AjaxRequestManager {

    protected JavascriptExecutor js;

    protected boolean active;

    protected int count;

    public AjaxRequestManager(WebDriver driver) {
        super();
        js = (JavascriptExecutor) driver;
        reset();
    }

    protected void reset() {
        active = false;
        count = 0;
    }

    /**
     * @since 7.2
     */
    public void begin() {
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
        sb.append(
                "  jsf.ajax.addOnEvent(function(e) {" + "if (e.status == 'begin') {window.NuxeoTestFaces.increment();}"
                        + "if (e.status == 'success') {window.NuxeoTestFaces.decrement();}" + "})");
        sb.append("}");
        sb.append("}");
        js.executeScript(sb.toString());
    }

    public void watchAjaxRequests() {
        begin();
    }

    /**
     * @since 7.2
     */
    public void end() {
        waitUntil((new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                Boolean res = (Boolean) js.executeScript("return window.NuxeoTestFaces.finished();");
                return res;
            }
        }));
    }

    public void waitForAjaxRequests() {
        end();
    }

    /**
     * @since 7.1
     */
    public void waitForJQueryRequests() {
        waitUntil(new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                Boolean res = (Boolean) ((JavascriptExecutor) driver).executeScript("return jQuery.active == 0;");
                return res;
            }
        });
    }

    /**
     * Wait for any pending request of the javascript client. Compatible with client 0.24.0 or greater.
     *
     * @since 8.2
     */
    public void waitForJsClient() {
        waitUntil(new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                final StringBuilder sb = new StringBuilder();
                sb.append("if (window.isNxJsClientActive === undefined) {");
                sb.append("  window.isNxJsClientActive=function() {");
                sb.append("    var result=false;");
                sb.append("    jQuery('nuxeo-connection').each(function() {");
                sb.append("      if(this.active){return result=true;}");
                sb.append("    });");
                sb.append("    return result;");
                sb.append("  }");
                sb.append("};");
                sb.append("return window.isNxJsClientActive();");
                Boolean res = (Boolean) ((JavascriptExecutor) driver).executeScript(sb.toString());
                return !res;
            }
        });
    }

    private void waitUntil(Function<WebDriver, Boolean> function) {
        Locator.waitUntilGivenFunctionIgnoring(function, NoSuchElementException.class);
    }
}
