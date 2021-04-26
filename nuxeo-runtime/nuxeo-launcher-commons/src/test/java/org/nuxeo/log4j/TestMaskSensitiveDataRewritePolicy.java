/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.log4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @since 11.5
 */
public class TestMaskSensitiveDataRewritePolicy {

    protected MaskSensitiveDataRewritePolicy policy = new MaskSensitiveDataRewritePolicy();

    @Test
    public void testMaskSensitiveAWS() {
        String testStr = "Should replace this AKIAI53OIMNYFFMFTEST key";
        assertEquals("Should replace this AKIAI53-AWS_KEY-TEST key", policy.maskSensitive(testStr));

        testStr = "Should replace this ASIAI53OIMNYFFMFTEST key";
        assertEquals("Should replace this ASIAI53-AWS_KEY-TEST key", policy.maskSensitive(testStr));
    }

    @Test
    public void testMaskSensitiveGCP() {
        String testStr = "Should replace this AIzaSyCKm1FJhzxI3eo8DX4PKLwXF4PCca_TEST key";
        assertEquals("Should replace this AIzaSyC-GCP_KEY-TEST key", policy.maskSensitive(testStr));
    }

    @Test
    public void testMaskSensitiveCreditCard() {
        String testStr = "Should not replace this 1321567809876543 card";
        assertEquals(testStr, policy.maskSensitive(testStr));

        testStr = "Should replace this 3548767486066506 card";
        assertEquals("Should replace this 3548-CRED-CARD-XXXX card", policy.maskSensitive(testStr));

        testStr = "Should replace this 3548-7674-8606-6506 card";
        assertEquals("Should replace this 3548-CRED-CARD-XXXX card", policy.maskSensitive(testStr));

        testStr = "Should replace this 3548 7674 8606 6506 card";
        assertEquals("Should replace this 3548-CRED-CARD-XXXX card", policy.maskSensitive(testStr));

        // Invalid card
        testStr = "Should not replace this 3548-7684-8606-6506 card";
        assertEquals(testStr, policy.maskSensitive(testStr));

        // Discover card
        testStr = "Should replace this 6011171308542342 or 6011 1713 0854 2342";
        assertEquals("Should replace this 6011-CRED-CARD-XXXX or 6011-CRED-CARD-XXXX", policy.maskSensitive(testStr));

        // Visa card
        testStr = "Should replace this VISA 4246188764124524 card";
        assertEquals("Should replace this VISA 4246-CRED-CARD-XXXX card", policy.maskSensitive(testStr));

        // Maestro card
        testStr = "Should replace this Maestro 4246188764124524 card";
        assertEquals("Should replace this Maestro 4246-CRED-CARD-XXXX card", policy.maskSensitive(testStr));

        // American Express card
        testStr = "Should replace this American Express 371449635398431 card";
        assertEquals("Should replace this American Express 3714-CRED-CARD-XXXX card", policy.maskSensitive(testStr));

        testStr = "Should replace this American Express 3714 496353 98431 card";
        assertEquals("Should replace this American Express 3714-CRED-CARD-XXXX card", policy.maskSensitive(testStr));

        // China UnionPay card
        testStr = "Should replace this UnionPay 8171 9999 0000 0000 021 card";
        assertEquals("Should replace this UnionPay 8171-CRED-CARD-XXXX card", policy.maskSensitive(testStr));

        testStr = "Should replace this UnionPay 817199 9900000000021 card";
        assertEquals("Should replace this UnionPay 8171-CRED-CARD-XXXX card", policy.maskSensitive(testStr));
    }

    // NXP-30304
    @Test
    public void testMaskSensitivePassword() {
        String testStr = "Should replace this password=secret";
        assertEquals("Should replace this password=***", policy.maskSensitive(testStr));

        testStr = "Should replace this something.password=secret";
        assertEquals("Should replace this something.password=***", policy.maskSensitive(testStr));

        testStr = "Should replace this password.something=secret";
        assertEquals("Should replace this password.something=***", policy.maskSensitive(testStr));

        testStr = "Should replace this something.password.something=secret";
        assertEquals("Should replace this something.password.something=***", policy.maskSensitive(testStr));

        testStr = "Should replace this password=secret, and this password=secret";
        assertEquals("Should replace this password=***, and this password=***", policy.maskSensitive(testStr));

        testStr = "Should replace this superPassword=secret";
        assertEquals("Should replace this superPassword=***", policy.maskSensitive(testStr));
    }

    @Test
    public void testIsValidCardWithAmericanExpress() {
        assertTrue(policy.isValidCreditCard("371449635398431"));
        assertFalse(policy.isValidCreditCard("371449635398432"));
    }

    @Test
    public void testIsValidCardWithMaestro() {
        assertTrue(policy.isValidCreditCard("4246188764124524"));
        assertFalse(policy.isValidCreditCard("4246188764124525"));
    }

    @Test
    public void testIsValidCardWithUnionPay() {
        assertTrue(policy.isValidCreditCard("8171999900000000021"));
        assertFalse(policy.isValidCreditCard("8171999900000000022"));
    }

    @Test
    public void testIsValidCardWithVisa() {
        assertTrue(policy.isValidCreditCard("4246188764124524"));
        assertFalse(policy.isValidCreditCard("4246188764124525"));
    }
}
