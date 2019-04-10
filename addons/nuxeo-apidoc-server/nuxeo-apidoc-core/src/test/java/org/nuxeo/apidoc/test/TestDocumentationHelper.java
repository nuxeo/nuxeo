/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.apidoc.test;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

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
        assertEquals("<p>\nfoo\n</p>\n" //
                + "<p>\nbar</p>", //
                DocumentationHelper.getHtml("foo\n\nbar"));
        assertEquals("<p>\nfoo\n</p>\n" //
                + "<p>\nbar</p>", //
                DocumentationHelper.getHtml("foo\n\n<br/>\nbar"));
        assertEquals("<p>\nfoo\n</p>\n" //
                + "<p>" //
                + "<pre><code>bar\n" //
                + "</code></pre></p>\n" //
                + "<p>\nbaz</p>", //
                DocumentationHelper.getHtml("foo\n<code>\nbar\n</code>\nbaz"));
        assertEquals("<p>\nfoo\n<ul>\n" //
                + "<li>bar</li>\n</ul></p>",
                DocumentationHelper.getHtml("foo\n<ul>\n<li>bar</li>\n</ul>\n"));
        assertEquals("<p>\nfoo\n</p>\n" //
                + "<p>\nbar</p>", //
                DocumentationHelper.getHtml("foo\n@author you\nbar"));
    }
}
