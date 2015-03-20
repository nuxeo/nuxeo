/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ftest.cap;

import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.restapiDoc.RestApiDocBasePage;

/**
 *
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
