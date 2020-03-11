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
 */
package org.nuxeo.functionaltests;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;

import com.google.common.base.Function;

/**
 * @since 7.10
 */
public class EventListener {

    private static int COUNT = 0;

    private final String id = "evt_listener_" + COUNT++;

    private final String event;

    private final String selector;

    private final JavascriptExecutor js;

    public EventListener(WebDriver driver, String event, String selector) {
        super();
        this.event = event;
        this.selector = selector;
        js = (JavascriptExecutor) driver;
        listen();
    }

    public void listen() {
        StringBuilder sb = new StringBuilder();
        sb.append("window.")
          .append(id)
          .append(" = 0;")
          .append("var els = document.querySelectorAll('")
          .append(selector)
          .append("');")
          .append("for (var i=0; i<els.length; i++) {")
          .append("  els[i].addEventListener('")
          .append(event)
          .append("', function(e) { window.")
          .append(id)
          .append("++; });")
          .append("}");
        js.executeScript(sb.toString());
    }

    public void waitCalled() {
        waitCalled(1);
    }

    public void waitCalled(int times) {
        waitUntil(driver -> (Boolean) js.executeScript("return window." + id + " == " + times + ";"));
    }

    public void reset() {
        js.executeScript("window." + id + " = 0;");
    }

    private void waitUntil(Function<WebDriver, Boolean> function) {
        Locator.waitUntilGivenFunctionIgnoring(function, NoSuchElementException.class);
    }

}
