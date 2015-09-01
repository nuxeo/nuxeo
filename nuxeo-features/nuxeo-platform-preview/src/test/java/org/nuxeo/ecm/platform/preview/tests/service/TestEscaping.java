/*
 * (C) Copyright 2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.preview.tests.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.preview.adapter.HtmlPreviewer;
import org.nuxeo.ecm.platform.preview.adapter.PlainTextPreviewer;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.platform.htmlsanitizer")
public class TestEscaping {

    @Test
    public void testPlainTextPreviewerEscaping() {
        PlainTextPreviewer previewer = new PlainTextPreviewer();
        assertEquals("<pre>foo</pre>", previewer.htmlContent("foo"));
        assertEquals("<pre>&lt;b>foo&lt;/b></pre>", previewer.htmlContent("<b>foo</b>"));
        assertEquals("<pre>foo<br/>bar</pre>", previewer.htmlContent("foo\nbar"));
        assertEquals("<pre>a &amp;&amp; b &lt; c</pre>", previewer.htmlContent("a && b < c"));
    }

    @Test
    public void testHtmlPreviewerEscaping() {
        HtmlPreviewer previewer = new HtmlPreviewer();
        assertEquals("<b>foo</b>", previewer.htmlContent("<b>foo</b>"));
        assertTrue(StringUtils.isBlank(previewer.htmlContent("<script>alert(1)</script>")));
    }

}
