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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ftest.cap;

import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.restapiDoc.RestApiDocBasePage;

/**
 * @since 7.3
 */
public class ITRestApiTestDoc extends AbstractTest {

    private static final String REST_API_DOC_URL = "/api/v1/doc";

    @Test
    public void testJavascriptError() throws UserNotConnectedException {
        login();
        // just loading the page will detect javascript error if any
        get(NUXEO_URL + REST_API_DOC_URL, RestApiDocBasePage.class);
        logout();
    }
}
