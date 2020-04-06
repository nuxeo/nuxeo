/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.lib.stream.tests.log;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.nuxeo.lib.stream.log.Name.NAMESPACE_GLOBAL;

import org.junit.Test;
import org.nuxeo.lib.stream.log.Name;

/**
 * @since 11.1
 */
public class TestName {

    @Test
    public void testValid() {
        assertEquals("bar", Name.ofUrn("bar").getUrn());
        assertEquals("bar", Name.ofUrn("bar").getId());
        assertEquals("bar", Name.idOfUrn("bar"));
        assertEquals(NAMESPACE_GLOBAL, Name.ofUrn("bar").getNamespace());
        assertEquals("bar", Name.ofUrn("bar").getName());

        assertEquals(Name.ofUrn("bar"), Name.ofUrn(Name.ofUrn("bar").getUrn()));
        assertEquals(Name.ofUrn("bar"), Name.ofId(Name.ofId("bar").getId()));

        assertEquals("1", Name.idOfUrn("1"));
        assertEquals("fooBar_1", Name.idOfUrn("fooBar_1"));

        assertEquals("something/bar", Name.ofUrn("something/bar").getUrn());
        assertEquals("something-bar", Name.ofUrn("something/bar").getId());
        assertEquals("something", Name.ofUrn("something/bar").getNamespace());
        assertEquals("bar", Name.ofUrn("something/bar").getName());

        assertEquals("something-fooBar", Name.ofUrn("something/fooBar").getId());
        assertEquals("something-foo-bar", Name.ofUrn("something/foo-bar").getId());
        assertEquals("some_thing-foo_bar-123", Name.ofUrn("some_thing/foo_bar-123").getId());

        String urn = "a_name_space/with-name";
        assertEquals(Name.ofUrn(urn), Name.ofId(Name.ofUrn(urn).getId()));
    }

    @Test
    public void testInvalidNamespace() {
        try {
            Name.ofUrn("");
            fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            Name.of("namespace-with-dash", "foo");
            fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            Name.ofUrn("namespace-with-dash/foo");
            fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            Name.ofUrn("namespace.with.dots/foo");
            fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            Name.ofUrn("namespace with a space/name-with-a.dot");
            fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            Name.ofUrn("a-name-without-namespace-with-dash");
            fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            Name.ofUrn("foo/");
            fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            Name.ofUrn("foo/");
            fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            Name.ofId(".");
            fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testInvalidLogName() {
        try {
            Name.of("namespace", "log with space");
            fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            Name.of("namespace", "log.with.dot");
            fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

}
