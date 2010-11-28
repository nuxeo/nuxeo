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

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.web.Browser;
import org.nuxeo.runtime.test.runner.web.BrowserFamily;
import org.nuxeo.runtime.test.runner.web.HomePage;

import com.google.inject.Inject;

// should fix in webengine web-types to add a dynamic mode
@Ignore("This is working only if admin module is loaded from a jar -> otherwise webengine types are missing from META-INF")
@RunWith(FeaturesRunner.class)
@Features(WebEngineFeature.class)
@Browser(type=BrowserFamily.FIREFOX)
@HomePage(type=WebEngineHomePage.class, url="http://localhost:8082")
@Jetty(port=8082)
//@RepositoryConfig(type=BackendType.H2)
public class WebEngineTest {

//    @Inject protected CoreSession session;
//
//    @Inject protected WebEngine we;

    @Inject
    private WebEngineHomePage home;

    @Test
    public void iCanRunWebEngine() throws Exception {
        LoginPage login = home.getLoginPage();
        assertTrue(home.hasModule("Admin"));
        assertFalse(login.isAuthenticated());
        login.ensureLogin("Administrator", "Administrator");
        assertTrue(login.isAuthenticated());
        AdminModulePage admin = home.getModulePage("Admin", AdminModulePage.class);
//        DocumentPage doc = admin.getDocumentPage("default-domain");
//        Assert.assertEquals("Default domain", doc.getTitle());
    }

}
