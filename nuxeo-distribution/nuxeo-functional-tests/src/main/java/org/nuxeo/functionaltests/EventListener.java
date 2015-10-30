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
 */
package org.nuxeo.functionaltests;

import com.google.common.base.Function;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import java.util.concurrent.TimeUnit;

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
        sb.append("window."+ id + " = 0;");
        sb.append("var els = document.querySelectorAll('" + selector +"');");
        sb.append("for (var i=0; i<els.length; i++) {");
        sb.append("  els[i].addEventListener('" + event + "', function(e) { window."+ id + "++; });");
        sb.append("}");
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
        Wait<WebDriver> wait = new FluentWait<WebDriver>(AbstractTest.driver).withTimeout(
            AbstractTest.LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS).pollingEvery(
            AbstractTest.POLLING_FREQUENCY_MILLISECONDS, TimeUnit.MILLISECONDS).ignoring(
            NoSuchElementException.class);
        wait.until(function);
    }

}
