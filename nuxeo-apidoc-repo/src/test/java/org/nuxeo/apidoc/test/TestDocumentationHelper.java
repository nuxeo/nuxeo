/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.apidoc.test;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.apidoc.documentation.DocumentationHelper;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestDocumentationHelper extends NXRuntimeTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.event");
        deployBundle("org.nuxeo.ecm.platform.htmlsanitizer");
    }

    @Test
    public void test() throws Exception {
        assertEquals("<p>\nfoo\n</p>" //
                + "<p>\nbar</p>", //
                DocumentationHelper.getHtml("foo\n\nbar"));
        assertEquals("<p>\nfoo\n</p>" //
                + "<p>\nbar</p>", //
                DocumentationHelper.getHtml("foo\n\n<br/>\nbar"));
        assertEquals("<p>\nfoo\n</p>" //
                + "<p></p><pre><code>bar\n" //
                + "</code></pre>" //
                + "<p>\nbaz</p>", //
                DocumentationHelper.getHtml("foo\n<code>\nbar\n</code>\nbaz"));
        assertEquals("<p>\nfoo\n</p><ul>" //
                + "<li>bar</li></ul>", //
                DocumentationHelper.getHtml("foo\n<ul>\n<li>bar</li>\n</ul>\n"));
        assertEquals("<p>\nfoo\n</p>" //
                + "<p>\nbar</p>", //
                DocumentationHelper.getHtml("foo\n@author you\nbar"));
    }
}
