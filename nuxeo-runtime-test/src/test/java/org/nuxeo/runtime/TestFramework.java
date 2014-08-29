/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.runtime;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.9.4-JSF2
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
