/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.blob.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.blob.s3.LogTracingHelper.assertEqualsLists;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class LogTracingHelperTest {

    @Test
    public void testAssertEqualsLists() {
        Map<String, String> context = new HashMap<>();
        assertEqualsLists(Arrays.asList("foo", "bar"), Arrays.asList("foo", "bar"), context);
        // capture variable
        assertEqualsLists(Arrays.asList("foo${NAME}bar"), Arrays.asList("foogeebar"), context);
        assertEquals("gee", context.get("NAME"));
        // match variable
        assertEqualsLists(Arrays.asList("hello${NAME}"), Arrays.asList("hellogee"), context);
    }

    @Test
    public void testAssertEqualsListsFailures() {
        Map<String, String> context = new HashMap<>();
        try {
            assertEqualsLists(Arrays.asList("foo"), Arrays.asList("bar"), context);
            fail();
        } catch (AssertionError e) {
            assertEquals("at line 1 expected:<[foo]> but was:<[bar]>", e.getMessage());
        }
        try {
            assertEqualsLists(Arrays.asList(), Arrays.asList("bar"), context);
            fail();
        } catch (AssertionError e) {
            assertEquals("at line 1: Unexpected line: bar", e.getMessage());
        }
        try {
            assertEqualsLists(Arrays.asList("foo"), Arrays.asList(), context);
            fail();
        } catch (AssertionError e) {
            assertEquals("at line 1: Missing line: foo", e.getMessage());
        }
        // failed captures
        try {
            assertEqualsLists(Arrays.asList("foo${VAR}"), Arrays.asList("bar"), context);
            fail();
        } catch (AssertionError e) {
            assertEquals("at line 1: Could not match expected:<[foo${VAR}]> but was:<[bar]>", e.getMessage());
        }
        context.put("VAR", "foo");
        try {
            assertEqualsLists(Arrays.asList("${VAR}"), Arrays.asList("bar"), context);
            fail();
        } catch (AssertionError e) {
            assertEquals("at line 1 expected:<[foo]> but was:<[bar]>", e.getMessage());
        }
    }

}
