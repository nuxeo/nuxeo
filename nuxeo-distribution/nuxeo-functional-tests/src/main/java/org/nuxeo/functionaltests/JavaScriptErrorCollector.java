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
