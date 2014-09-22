/*
 * (C) Copyright 2010-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Haines
 */
package org.nuxeo.ecm.ui.web.auth.digest;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * Nuxeo Authenticator for HTTP Digest Access Authentication (RFC 2617).
 *
 * @since 5.8
 */
public class DigestAuthenticatorTest {

    @Test
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
