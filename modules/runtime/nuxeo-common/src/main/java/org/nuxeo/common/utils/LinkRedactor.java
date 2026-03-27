/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.common.utils;

import java.util.regex.Pattern;

/**
 * Utility class for redacting linkable content from plain text before rendering in contexts where auto-linking may
 * occur (such as email clients).
 * <p>
 * Email clients like Gmail, Outlook and Apple Mail automatically convert plain-text URLs and email addresses into
 * clickable links. This class provides methods to redact such patterns from user-provided text to prevent content
 * spoofing.
 *
 * @since 2025.18
 */
public final class LinkRedactor {

    /** Replacement text for redacted URLs. */
    public static final String LINK_REMOVED = "[link removed]";

    /** Replacement text for redacted email addresses. */
    public static final String EMAIL_REMOVED = "[email removed]";

    /** Pattern matching URLs: {@code http(s)://}, {@code ftp://}, and bare {@code www.} domains. */
    private static final Pattern URL_PATTERN = Pattern.compile("(?:https?://|ftp://|\\bwww\\.)\\S+",
            Pattern.CASE_INSENSITIVE);

    /** Pattern matching email addresses. */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}");

    // utility class
    private LinkRedactor() {
    }

    /**
     * Redacts URLs and email addresses from the given plain text.
     * <p>
     * URLs are replaced with {@value #LINK_REMOVED} and email addresses with {@value #EMAIL_REMOVED}. This prevents
     * email clients from auto-linking user-provided content, which could be used for phishing.
     *
     * @param text the text to sanitize, may be {@code null}
     * @return the sanitized text with URLs and emails redacted, or {@code null} if input was {@code null}
     */
    public static String redactLinks(String text) {
        if (text == null) {
            return null;
        }
        var result = URL_PATTERN.matcher(text).replaceAll(LINK_REMOVED);
        return EMAIL_PATTERN.matcher(result).replaceAll(EMAIL_REMOVED);
    }
}
