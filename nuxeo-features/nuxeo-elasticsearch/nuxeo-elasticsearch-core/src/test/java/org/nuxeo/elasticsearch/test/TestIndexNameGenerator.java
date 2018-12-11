/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.elasticsearch.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.nuxeo.elasticsearch.api.IndexNameGenerator;
import org.nuxeo.elasticsearch.core.IncrementalIndexNameGenerator;

/**
 * @since 9.3
 */
public class TestIndexNameGenerator {

    @Test
    public void testIncrementalAliasNameResolver() throws Exception {
        IndexNameGenerator resolver = new IncrementalIndexNameGenerator();
        assertEquals("foo-0000", resolver.getNextIndexName("foo", null));
        assertEquals("foo-0000", resolver.getNextIndexName("foo", ""));
        assertEquals("foo-0002", resolver.getNextIndexName("foo", "foo-0001"));
        assertEquals("foo-bar-0000", resolver.getNextIndexName("foo-bar", null));
        assertEquals("foo-bar-0002", resolver.getNextIndexName("foo-bar", "foo-bar-0001"));
        assertEquals("a-0000", resolver.getNextIndexName("a", null));
    }
}
