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
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests;

import static org.junit.Assert.fail;

import java.util.List;

import org.openqa.selenium.WebDriver;

import net.jsourcerer.webdriver.jserrorcollector.JavaScriptError;

/**
 * Helper class to collect JavaScript errors on a page.
 *
 * @since 8.1
 */
public class JavaScriptErrorCollector {

    protected final WebDriver driver;

    public JavaScriptErrorCollector(WebDriver driver) {
        super();
        this.driver = driver;
    }

    /**
     * Throws an {@link AssertionError} when JavaScript errors are detected on current page.
     */
    public void checkForErrors() {
        if (driver != null) {
            List<JavaScriptError> jsErrors = JavaScriptError.readErrors(driver);
            if (jsErrors != null && !jsErrors.isEmpty()) {
                StringBuilder msg = new StringBuilder();
                int i = 0;
                for (JavaScriptError jsError : jsErrors) {
                    String error = jsError.getErrorMessage();
                    // skip error which is actually a warning for FF 42
                    if (error != null && error.startsWith("mutating the [[Prototype]] of an object")) {
                        continue;
                    }
                    if (i != 0) {
                        msg.append(", ");
                    }
                    i++;
                    msg.append("\"").append(error).append("\"");
                    msg.append(" at ").append(jsError.getSourceName());
                    msg.append(" line ").append(jsError.getLineNumber());
                }
                if (i > 0) {
                    msg.append("]");
                    msg.insert(0, jsErrors.size() + " Javascript error(s) detected: " + "[");
                    fail(msg.toString());
                }
            }
        }

    }

}
