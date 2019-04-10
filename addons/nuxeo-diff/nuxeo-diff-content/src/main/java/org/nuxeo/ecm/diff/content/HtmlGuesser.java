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
