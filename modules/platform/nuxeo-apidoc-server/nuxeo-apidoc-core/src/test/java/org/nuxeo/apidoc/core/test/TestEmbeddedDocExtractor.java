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
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.core.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.nuxeo.apidoc.introspection.EmbeddedDocExtractor;

/**
 * @since 11.1
 */
public class TestEmbeddedDocExtractor {

    @Test
    public void testIsReadme() {
        assertEquals(0, EmbeddedDocExtractor.isReadme(null));
        assertEquals(0, EmbeddedDocExtractor.isReadme(""));
        assertEquals(0, EmbeddedDocExtractor.isReadme("foo"));
        assertEquals(0, EmbeddedDocExtractor.isReadme("doc/foo"));
        assertEquals(0, EmbeddedDocExtractor.isReadme("doc-parent/foo"));
        assertEquals(1, EmbeddedDocExtractor.isReadme("doc/Readme.md"));
        assertEquals(1, EmbeddedDocExtractor.isReadme("doc/ReadMe.md"));
        assertEquals(1, EmbeddedDocExtractor.isReadme("doc/Readme-foo.md"));
        assertEquals(2, EmbeddedDocExtractor.isReadme("doc-parent/Readme.md"));
        assertEquals(2, EmbeddedDocExtractor.isReadme("doc-parent/ReadMe.md"));
        assertEquals(2, EmbeddedDocExtractor.isReadme("doc-parent/ReadMe-foo.md"));
        assertEquals(0, EmbeddedDocExtractor.isReadme("doc/Readme.txt"));
        assertEquals(0, EmbeddedDocExtractor.isReadme("doc/Readme.html"));
        assertEquals(0, EmbeddedDocExtractor.isReadme("doc/Readme-sample.ascidoc"));
        assertEquals(0, EmbeddedDocExtractor.isReadme("doc/Readme.md.foo"));
        assertEquals(0, EmbeddedDocExtractor.isReadme("doc-parent/Readme.txt"));
        assertEquals(0, EmbeddedDocExtractor.isReadme("doc-parent/Readme.html"));
    }
}
