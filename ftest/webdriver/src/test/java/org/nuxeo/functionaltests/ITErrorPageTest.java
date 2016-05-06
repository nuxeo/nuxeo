/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Yannis JULIENNE
 */
package org.nuxeo.functionaltests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.nuxeo.functionaltests.pages.ErrorPage;
import org.openqa.selenium.By;

/**
 * Coverage Navigation tests.
 */
public class ITErrorPageTest extends AbstractTest {

    private static final String GO_TO_ERROR_PAGE = "Go to error page";

    public static final String MESSAGE_SUFFIX_SHORT = " Click on the following links to go back to the application.";

    public static final String MESSAGE_SUFFIX = " Click on the following links to get more information or go back to the application.";

    private static final String PAGE_NOT_FOUND_TITLE = "Sorry, the page you requested cannot be found.";

    private static final String PAGE_NOT_FOUND_MESSAGE = "The page you requested has been moved or deleted.";

    private static final String ERROR_OCCURED_TITLE = "An error occurred.";

    private static final String ERROR_OCCURED_MESSAGE = "An unexpected error occurred.";

    @Test
    public void testInvalidHTMLError() throws Exception {
        driver.get(NUXEO_URL);
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText(GO_TO_ERROR_PAGE));
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText("Error invalid html"));
        checkErrorPage(ERROR_OCCURED_TITLE, ERROR_OCCURED_MESSAGE, true, false);
    }

    @Test
    public void testPageNotFound() throws Exception {
        driver.get(NUXEO_URL + "/notfound");
        ErrorPage errorPage = asPage(ErrorPage.class);
        errorPage.checkTitle(PAGE_NOT_FOUND_TITLE);
        errorPage.checkMessage(PAGE_NOT_FOUND_MESSAGE + MESSAGE_SUFFIX_SHORT);
        assertTrue(errorPage.hasBackToHomeLink());
        assertTrue(errorPage.hasLogOutLink());
    }

    private void checkErrorPage(String title, String message, boolean backToHomeAndLogOutLinks,
            boolean showStackTraceAndContextDumpLinks) {
        ErrorPage errorPage = asPage(ErrorPage.class);
        errorPage.checkTitle(title);
        errorPage.checkMessage(message + MESSAGE_SUFFIX);
        assertEquals(backToHomeAndLogOutLinks, errorPage.hasBackToHomeLink() && errorPage.hasLogOutLink());
        assertEquals(showStackTraceAndContextDumpLinks,
                errorPage.hasShowStackTraceLink() && errorPage.hasShowContextDumpLink());
    }
}
