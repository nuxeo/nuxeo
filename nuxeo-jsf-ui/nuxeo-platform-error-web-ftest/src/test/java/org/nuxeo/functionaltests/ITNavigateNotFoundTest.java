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

import org.junit.Test;
import org.nuxeo.functionaltests.pages.ErrorPage;
import org.nuxeo.functionaltests.pages.LoginPage;

import static org.nuxeo.functionaltests.pages.ErrorPage.ERROR_OCCURED_TITLE;
import static org.nuxeo.functionaltests.pages.ErrorPage.MUST_BE_AUTH_MESSAGE;
import static org.nuxeo.functionaltests.pages.ErrorPage.MESSAGE_SUFFIX_SHORT;
import static org.nuxeo.functionaltests.pages.ErrorPage.PAGE_NOT_FOUND_TITLE;
import static org.nuxeo.functionaltests.pages.ErrorPage.PAGE_NOT_FOUND_MESSAGE;
import static org.nuxeo.functionaltests.pages.ErrorPage.DOCUMENT_NOT_FOUND_MESSAGE;

/**
 * Navigation not found tests.
 */
public class ITNavigateNotFoundTest extends AbstractTest {

    @Test
    public void testPageNotFound() throws Exception {
        driver.get(NUXEO_URL + "/doc/not/found@view_documents");
        asPage(ErrorPage.class).checkErrorPage(PAGE_NOT_FOUND_TITLE, PAGE_NOT_FOUND_MESSAGE, true, false, false,
                MESSAGE_SUFFIX_SHORT);
    }

    @Test
    public void testDocumentNotFound() throws Exception {
        driver.get(NUXEO_URL + "/nxpath/foo@view_documents");
        ErrorPage errorPage = asPage(ErrorPage.class);
        errorPage.checkErrorPage(ERROR_OCCURED_TITLE, MUST_BE_AUTH_MESSAGE, false, false, true, "");
        driver.get(NUXEO_URL + "/nxpath/default/foo@view_documents");
        errorPage = asPage(ErrorPage.class);
        errorPage.checkErrorPage(ERROR_OCCURED_TITLE, MUST_BE_AUTH_MESSAGE, false, false, true, "");
        LoginPage loginPage = errorPage.goSignIn();
        loginPage.login("Administrator", "Administrator");
        driver.get(NUXEO_URL + "/nxpath/default/foo@view_documents");
        errorPage.checkErrorPage(ERROR_OCCURED_TITLE, DOCUMENT_NOT_FOUND_MESSAGE, true, false, false, "");
    }

    @Test
    public void testViewNotFound() throws Exception {
        driver.get(NUXEO_URL + "//nxpath/default-domain@foo");
        asPage(ErrorPage.class).checkErrorPage(PAGE_NOT_FOUND_TITLE, PAGE_NOT_FOUND_MESSAGE, true, false, false,
                MESSAGE_SUFFIX_SHORT);
    }

}
