/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.webengine.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.test.web.BrowserConfig;
import org.nuxeo.ecm.webengine.test.web.BrowserRunner;
import org.nuxeo.ecm.webengine.test.web.pages.WebEngineHomePage;
import org.openqa.selenium.WebDriver;

import com.google.inject.Inject;

@Ignore("failing for now...")
@RunWith(BrowserRunner.class)
public class WebEngineTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected WebEngine we;

    @Inject
    private BrowserConfig browserConfig;

    private WebDriver webDriver;

    private WebEngineHomePage home;

    @Before
    public void login() {
        webDriver = browserConfig.getTestDriver();
        home = new WebEngineHomePage(webDriver, "localhost", "11111");
        home.reload();
    }

    @Test
    public void iCanRunWebEngine() throws Exception {
        assertTrue(home.hasApplication("Admin"));
        assertFalse(home.isLogged());

        home.loginAs("Administrator", "Administrator");
        assertTrue(home.isLogged());
    }

}
