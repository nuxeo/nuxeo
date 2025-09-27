/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */

package org.nuxeo.ecm.platform.shibboleth.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.web.common.MockHttpServletRequest;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.login.shibboleth")
@Deploy("org.nuxeo.ecm.platform.login.shibboleth:OSGI-INF/test-shibboleth-authentication-contrib.xml")
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
        var request = MockHttpServletRequest.init();
        request.whenGetHeaderThenReturn("uid", "test");
        request.whenGetHeaderThenReturn("uid1", "test1");
        request.whenGetHeaderThenReturn("uid2", "test2");

        request.whenGetHeaderThenReturn("shib-identity-provider", "url1");
        assertEquals("test1", service.getUserID(request.mock()));

        request.whenGetHeaderThenReturn("shib-identity-provider", "url2");
        assertEquals("test2", service.getUserID(request.mock()));

        request.whenGetHeaderThenReturn("shib-identity-provider", "another.url");
        assertEquals("test", service.getUserID(request.mock()));
    }

    @Test
    public void testUserMetada() {
        var request = MockHttpServletRequest.init();
        request.whenGetHeaderThenReturn("uid", "FrÃ©dÃ©ric");
        request.whenGetHeaderThenReturn("uid1", "value1");
        request.whenGetHeaderThenReturn("uid2", "value2");

        String idField = "username";

        request.whenGetHeaderThenReturn("shib-identity-provider", "url1");
        assertEquals("value1", service.getUserMetadata(idField, request.mock()).get(idField));

        request.whenGetHeaderThenReturn("shib-identity-provider", "url2");
        assertEquals("value2", service.getUserMetadata(idField, request.mock()).get(idField));

        request.whenGetHeaderThenReturn("shib-identity-provider", "anotherUrl");
        assertEquals("Frédéric", service.getUserMetadata(idField, request.mock()).get(idField));
    }
}
