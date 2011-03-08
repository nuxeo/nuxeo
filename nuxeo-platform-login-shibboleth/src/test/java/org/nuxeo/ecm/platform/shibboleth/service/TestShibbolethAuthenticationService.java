/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */

package org.nuxeo.ecm.platform.shibboleth.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(type = BackendType.H2, init = DefaultRepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy( { "org.nuxeo.ecm.platform.login.shibboleth" })
@LocalDeploy("org.nuxeo.ecm.platform.login.shibboleth:OSGI-INF/test-shibboleth-authentication-contrib.xml")
public class TestShibbolethAuthenticationService {

    @Inject
    protected ShibbolethAuthenticationService service;

    @Test
    public void serviceRegistration() {
        assertNotNull(service);
    }

    @Test
    public void testLoginURL() throws UnsupportedEncodingException {
        String redirectURL = "https://test.nuxeo.org";
        String loginURL = service.getLoginURL(redirectURL);

        String encodedRedirectUrl = URLEncoder.encode(redirectURL, "UTF-8");
        assertEquals("https://host/Shibboleth.sso/WAYF?target=" + encodedRedirectUrl, loginURL);
    }

    @Test
    public void testLogoutURL() throws UnsupportedEncodingException {
        String redirectURL = "https://test.nuxeo.org";
        String logoutURL = service.getLogoutURL(redirectURL);

        String encodedRedirectUrl = URLEncoder.encode(redirectURL, "UTF-8");
        assertEquals("https://host/Shibboleth.sso/Logout?return=" + encodedRedirectUrl, logoutURL);
    }

    @Test
    public void testUidHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setHeader("uid", "test");
        request.setHeader("uid1", "test1");
        request.setHeader("uid2", "test2");

        request.setHeader("shib-identity-provider", "url1");
        assertEquals("test1", service.getUserID(request));

        request.setHeader("shib-identity-provider", "url2");
        assertEquals("test2", service.getUserID(request));

        request.setHeader("shib-identity-provider", "another.url");
        assertEquals("test", service.getUserID(request));
    }

    @Test
    public void testUserMetada() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setHeader("uid", "test");
        request.setHeader("uid1", "value1");
        request.setHeader("uid2", "value2");

        String idField = "username";

        request.setHeader("shib-identity-provider", "url1");
        assertEquals("value1", service.getUserMetadata(idField,
                request).get(idField));

        request.setHeader("shib-identity-provider", "url2");
        assertEquals("value2", service.getUserMetadata(idField,
                request).get(idField));

        request.setHeader("shib-identity-provider", "anotherUrl");
        assertEquals("test", service.getUserMetadata(idField,
                request).get(idField));
    }
}
