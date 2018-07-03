/*
t * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.jwt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.jwt.JWTClaims.CLAIM_ISSUER;
import static org.nuxeo.ecm.jwt.JWTClaims.CLAIM_SUBJECT;
import static org.nuxeo.ecm.jwt.JWTServiceImpl.NUXEO_ISSUER;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.api.local.LoginStack;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.jwt")
public class TestJWTService {

    // secret from XML, to check manual token creation
    protected static final String SECRET = "abracadabra";

    protected static final String USERNAME = "bob";

    @Inject
    protected JWTService service;

    @Before
    public void setUp() throws Exception {
        LoginStack loginStack = ClientLoginModule.getThreadLocalLogin();
        loginStack.push(new UserPrincipal(USERNAME, Collections.emptyList(), false, false), null, null);
    }

    @After
    public void teardown() throws Exception {
        LoginStack loginStack = ClientLoginModule.getThreadLocalLogin();
        loginStack.pop();
    }

    @Test
    @Deploy("org.nuxeo.ecm.jwt.tests:OSGI-INF/test-jwt-config.xml")
    public void testCreation() throws Exception {
        String token = service.newBuilder().withClaim("myclaim", Long.valueOf(123456)).build();
        Map<String, Object> claims = service.verifyToken(token);
        String subject = (String) claims.get(CLAIM_SUBJECT);
        assertEquals(USERNAME, subject);
        String issuer = (String) claims.get(CLAIM_ISSUER);
        assertEquals("nuxeo", issuer);
        Long myclaim = (Long) claims.get("myclaim");
        assertEquals(Long.valueOf(123456), myclaim);
    }

    @Test
    public void testCreationSecretNotConfigured() throws Exception {
        try {
            service.newBuilder().build();
            fail("should fail because service not configured");
        } catch (NuxeoException e) {
            assertEquals("JWTService secret not configured", e.getMessage());
        }
    }

    @Test
    public void testValidateSecretNotConfigured() throws Exception {
        assertNull(service.verifyToken("sometoken"));
    }

    @Test
    @Deploy("org.nuxeo.ecm.jwt.tests:OSGI-INF/test-jwt-config.xml")
    public void testExpiredToken() throws Exception {
        String token = service.newBuilder().withTTL(2).build();
        assertNotNull(service.verifyToken(token));
        Thread.sleep(3_000);
        assertNull(service.verifyToken(token));
    }

    @Test
    @Deploy("org.nuxeo.ecm.jwt.tests:OSGI-INF/test-jwt-config.xml")
    public void testCorruptedToken() throws Exception {
        String token = service.newBuilder().build();
        assertNull(service.verifyToken(token + "foobar"));
    }

    @Test
    @Deploy("org.nuxeo.ecm.jwt.tests:OSGI-INF/test-jwt-config.xml")
    public void testManualTokenCreation() throws Exception {
        String token = JWT.create() //
                          .withIssuer(NUXEO_ISSUER)
                          .withSubject(USERNAME)
                          .sign(Algorithm.HMAC512(SECRET));
        assertNotNull(service.verifyToken(token));
    }

    @Test
    @Deploy("org.nuxeo.ecm.jwt.tests:OSGI-INF/test-jwt-config.xml")
    public void testCorruptedTokenBadSecret() throws Exception {
        String token = JWT.create() //
                          .withIssuer(NUXEO_ISSUER)
                          .withSubject(USERNAME)
                          .sign(Algorithm.HMAC512("badsecret"));
        assertNull(service.verifyToken(token));
    }

    @Test
    @Deploy("org.nuxeo.ecm.jwt.tests:OSGI-INF/test-jwt-config.xml")
    public void testCorruptedTokenBadIssuer() throws Exception {
        String token = JWT.create() //
                          .withIssuer("badissuer")
                          .withSubject(USERNAME)
                          .sign(Algorithm.HMAC512(SECRET));
        assertNull(service.verifyToken(token));
    }

}
