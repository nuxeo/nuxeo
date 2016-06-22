/*
 * (C) Copyright 2010-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Haines
 */
package org.nuxeo.ecm.ui.web.auth.digest;

import java.util.Map;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Nuxeo Authenticator for HTTP Digest Access Authentication (RFC 2617).
 *
 * @since 5.8
 */
public class DigestAuthenticatorTest {

    @Test
    @Ignore("Regression on NXP-12830")
    public void testWithComma() {
        String uri = "/nuxeo/site/dav/Patricia/Documents/2/1425/AU/00/G511_Oct_09,_2013_68999.doc";
        check(uri);
    }

    @Test
    public void testWithoutComma() {
        String uri = "/nuxeo/site/dav/Patricia/Documents/2/1425/AU/00/G511_Oct_09_2013_68999.doc";
        check(uri);
    }

    protected void check(String uri) {
        String auth2 = "username=\"kirsty\",realm=\"NUXEO\",nonce=\"MTM4MTI4ODc4NDYyNTo0ZTcxNTcyYmNmNjI1YWMxOTk4MTllM2JhOTNmOTFjMw==\",uri=\""
                + uri
                + "\",cnonce=\"d30fb25c5345b787bccd677d1cb93bd6\",nc=00000001,response=\"c8b18e0e7e6a55fe6a72ada845f7f1c7\",qop=\"auth\"";
        Map<String, String> map = DigestAuthenticator.splitParameters(auth2);
        Assert.assertEquals(8, map.keySet().size());
        Assert.assertTrue(map.keySet().contains("uri"));
        Assert.assertEquals(uri, map.get("uri"));
    }

}
