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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles HTML detection in a string.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public final class HtmlGuesser {

    public static final String HTML_TAG_REGEXP = "<.*?>.*?</.*?>";

    /**
     * Checks if the specified text can be considered as HTML code.
     *
     * @param text the text
     * @return true, if the specified text contains at least one HTML tag.
     */
    public static boolean isHtml(String text) {
        Pattern pattern = Pattern.compile(HTML_TAG_REGEXP);
        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }
}
