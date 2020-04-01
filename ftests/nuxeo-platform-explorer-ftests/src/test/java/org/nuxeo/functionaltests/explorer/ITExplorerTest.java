/*
 * (C) Copyright 2014-2020 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests.explorer;

import static org.nuxeo.functionaltests.Constants.ADMINISTRATOR;

import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;

/**
 * Test explorer main webengine pages.
 *
 * @since 11.1
 */
public class ITExplorerTest extends AbstractTest {

    /**
     * Simple login, logout test, checking the home page is displayed without errors after login.
     */
    @Test
    public void testLoginLogout() throws UserNotConnectedException {
        getLoginPage().login(ADMINISTRATOR, ADMINISTRATOR);
        open("/site/distribution");
        // logout avoiding JS error check
        driver.get(NUXEO_URL + "/logout");
    }
}
