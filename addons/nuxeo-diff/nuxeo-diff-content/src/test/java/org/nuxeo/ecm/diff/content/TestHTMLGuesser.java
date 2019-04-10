/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.content;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests the {@link HtmlGuesser} class.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public class TestHTMLGuesser {

    /**
     * Tests {@link HtmlGuesser#isHtml(String)}.
     */
    @Test
    public void testDiffDefaultDisplayContrib() {

        assertFalse(HtmlGuesser.isHtml("This is plain text."));
        assertFalse(HtmlGuesser.isHtml("This text does not contain <well-formed> HTML tags."));
        assertTrue(HtmlGuesser.isHtml("This text contains <strong>HTML</strong> tags."));
    }
}
