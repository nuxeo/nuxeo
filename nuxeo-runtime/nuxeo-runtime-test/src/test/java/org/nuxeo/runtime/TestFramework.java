/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.runtime;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 6.0
 */
public class TestFramework {

    @Test
    public void testRuntimeNotInitializedException() {
        assertFalse(Framework.isInitialized());
        try {
            Framework.getProperty("foo");
            fail("Should have thrown an IllegalStateException");
        } catch (IllegalStateException e) {
        }
        try {
            Framework.isDevModeSet();
            fail("Should have thrown an IllegalStateException");
        } catch (IllegalStateException e) {
        }
        try {
            Framework.isBooleanPropertyTrue("foo");
            fail("Should have thrown an IllegalStateException");
        } catch (IllegalStateException e) {
        }
    }

}
