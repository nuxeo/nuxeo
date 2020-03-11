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

import java.util.ArrayList;
import java.util.Arrays;
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

    protected List<JavaScriptErrorIgnoreRule> ignores;

    public JavaScriptErrorCollector(WebDriver driver) {
        super();
        this.driver = driver;
        ignores = new ArrayList<>();
        // Add this error per default which are actually a warning for FF 42
        ignore(JavaScriptErrorIgnoreRule.startsWith("mutating the [[Prototype]] of an object"));
    }

    /**
     * @since 9.3
     */
    public static JavaScriptErrorCollector from(WebDriver driver) {
        return new JavaScriptErrorCollector(driver);
    }

    /**
     * @since 9.3
     */
    public JavaScriptErrorCollector ignore(JavaScriptErrorIgnoreRule... ignores) {
        this.ignores.addAll(Arrays.asList(ignores));
        return this;
    }

    /**
     * Throws an {@link AssertionError} when JavaScript errors are detected on current page.
     */
    public void checkForErrors() {
        if (driver == null) {
            return;
        }

        List<JavaScriptError> jsErrors = JavaScriptError.readErrors(driver);
        if (jsErrors != null && !jsErrors.isEmpty()) {
            StringBuilder msg = new StringBuilder();
            int i = 0;
            for (JavaScriptError jsError : jsErrors) {
                // skip errors that match an ignored rule
                if (ignores.stream().anyMatch(e -> e.isIgnored(jsError))) {
                    continue;
                }
                if (i != 0) {
                    msg.append(", ");
                }
                i++;
                msg.append("\"").append(jsError.getErrorMessage()).append("\"");
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

    /**
     * @since 9.3
     */
    public static class JavaScriptErrorIgnoreRule {
        protected String text = "";

        protected String source = "";

        protected JavaScriptErrorIgnoreRule(String text) {
            this.text = text;
        }

        /**
         * Ensure that error has to be ignored or not.
         */
        protected boolean isIgnored(JavaScriptError error) {
            if (error.getErrorMessage().startsWith(text)) {
                return error.getSourceName().startsWith(source);
            }

            return false;
        }

        public JavaScriptErrorIgnoreRule withSource(String source) {
            this.source = source;
            return this;
        }

        public static JavaScriptErrorIgnoreRule startsWith(String text) {
            return new JavaScriptErrorIgnoreRule(text);
        }

        public static JavaScriptErrorIgnoreRule fromSource(String source) {
            return new JavaScriptErrorIgnoreRule("").withSource(source);
        }
    }
}
