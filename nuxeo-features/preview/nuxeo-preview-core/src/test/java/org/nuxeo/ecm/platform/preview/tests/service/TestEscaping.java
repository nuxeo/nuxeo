/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.preview.tests.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
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
