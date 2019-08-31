/*
t * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.ui.web.auth.digest;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, MockitoFeature.class })
public class TestDigestAuthenticator {

    protected static final String USERNAME = "bob";

    protected static final String DIGEST_AUTH_DIR = "digestauth";

    protected static final String DIGEST_AUTH_SCHEMA = "digestauth";

    protected static final String DIGEST_AUTH_PASSWORD = "password";

    protected static final String HA1 = "myha1";

    @Mock
    @RuntimeService
    protected UserManager userManager;

    @Mock
    @RuntimeService
    protected DirectoryService dirService;

    @Test
    public void testValidDigest() throws Exception {
        doTestDigest(false);
    }

    @Test
    public void testBadDigest() throws Exception {
        doTestDigest(true);

    }

    protected void doTestDigest(boolean corrupted) throws Exception {
        DigestAuthenticator auth = new DigestAuthenticator();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = null; // unused
        when(request.getHeader(eq(AUTHORIZATION))).thenReturn(getAuthorizationHeader(corrupted));

        when(userManager.getDigestAuthDirectory()).thenReturn(DIGEST_AUTH_DIR);
        Directory digestAuthDir = mock(Directory.class);
        when(dirService.getDirectory(DIGEST_AUTH_DIR)).thenReturn(digestAuthDir);
        @SuppressWarnings("resource")
        Session dir = mock(Session.class);
        when(dirService.open(DIGEST_AUTH_DIR)).thenReturn(dir);
        when(dirService.getDirectorySchema(DIGEST_AUTH_DIR)).thenReturn(DIGEST_AUTH_SCHEMA);
        doNothing().when(dir).setReadAllColumns(true);
        doNothing().when(dir).close();
        when(dir.getPasswordField()).thenReturn(DIGEST_AUTH_PASSWORD);
        DocumentModel entry = mock(DocumentModel.class);
        when(dir.getEntry(USERNAME, true)).thenReturn(entry);
        when(entry.getProperty(DIGEST_AUTH_SCHEMA, DIGEST_AUTH_PASSWORD)).thenReturn(HA1);

        UserIdentificationInfo uii = auth.handleRetrieveIdentity(request, response);

        if (corrupted) {
            // auth plugin fails
            assertNull(uii);
        } else {
            // auth plugin succeeds
            assertNotNull(uii);
            assertEquals(USERNAME, uii.getUserName());
        }
    }

    protected String getAuthorizationHeader(boolean corrupted) {
        String realm = "NUXEO";
        String nonce = "MTM4MTI4ODc4NDYyNTo0ZTcxNTcyYmNmNjI1YWMxOTk4MTllM2JhOTNmOTFjMw==";
        // URI includes a comma, to check proper parsing
        String uri = "/nuxeo/site/dav/Patricia/Documents/2/1425/AU/00/G511_Oct_09,_2013_68999.doc";
        String cnonce = "d30fb25c5345b787bccd677d1cb93bd6";
        String nc = "00000001";
        String qpop = "auth";
        String response;
        if (corrupted) {
            response = "0000dead0000";
        } else {
            response = "e9b3cb9ae4a744666897781b3f8ebd5a";
        }
        return "Digest " //
                + "username=\"" + USERNAME + "\"," //
                + "realm=\"" + realm + "\"," //
                + "nonce=\"" + nonce + "\"," //
                + "uri=\"" + uri + "\"," //
                + "cnonce=\"" + cnonce + "\"," //
                + "nc=" + nc + "," //
                + "qop=\"" + qpop + "\"," //
                + "response=\"" + response + "\"";
    }

}
