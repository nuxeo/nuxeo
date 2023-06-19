/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.log4j;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @since 2021.40
 */
public class TestRedactor {

    protected Redactor redactor = new Redactor();

    @Test
    public void testIsValidCardWithAmericanExpress() {
        assertTrue(redactor.isValidCreditCard("371449635398431"));
        assertFalse(redactor.isValidCreditCard("371449635398432"));
    }

    @Test
    public void testIsValidCardWithMaestro() {
        assertTrue(redactor.isValidCreditCard("4246188764124524"));
        assertFalse(redactor.isValidCreditCard("4246188764124525"));
    }

    @Test
    public void testIsValidCardWithUnionPay() {
        assertTrue(redactor.isValidCreditCard("8171999900000000021"));
        assertFalse(redactor.isValidCreditCard("8171999900000000022"));
    }

    @Test
    public void testIsValidCardWithVisa() {
        assertTrue(redactor.isValidCreditCard("4246188764124524"));
        assertFalse(redactor.isValidCreditCard("4246188764124526"));
    }
}
