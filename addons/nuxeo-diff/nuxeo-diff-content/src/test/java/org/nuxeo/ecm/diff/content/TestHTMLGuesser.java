/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
