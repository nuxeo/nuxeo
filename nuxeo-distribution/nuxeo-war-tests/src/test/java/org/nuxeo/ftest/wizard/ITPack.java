/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Sun Seng David TAN
 *     Florent Guillaume
 *     Antoine Taillefer
 */
package org.nuxeo.ftest.wizard;

import static org.nuxeo.functionaltests.Constants.ADMINISTRATOR;

import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.NoUIPage;

/**
 * Tests login / logout.
 */
public class ITPack extends AbstractTest {

    @Test
    public void testLoginLogout() throws UserNotConnectedException {
        // Login
        getLoginPage().login(ADMINISTRATOR, ADMINISTRATOR, NoUIPage.class);
    }
}
