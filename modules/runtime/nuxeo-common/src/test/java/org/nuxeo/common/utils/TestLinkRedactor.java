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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.nuxeo.common.utils.LinkRedactor.EMAIL_REMOVED;
import static org.nuxeo.common.utils.LinkRedactor.LINK_REMOVED;

import org.junit.Test;

/**
 * @since 2025.18
 */
public class TestLinkRedactor {

    /** Asserts that {@code redactLinks(input)} produces {@code expected}. */
    protected static void assertRedacted(String expected, String input) {
        assertEquals(expected, LinkRedactor.redactLinks(input));
    }

    /** Asserts that {@code redactLinks(text)} returns the text unchanged. */
    protected static void assertUnchanged(String text) {
        assertEquals(text, LinkRedactor.redactLinks(text));
    }

    @Test
    public void testNull() {
        assertNull(LinkRedactor.redactLinks(null));
    }

    @Test
    public void testEmpty() {
        assertUnchanged("");
    }

    @Test
    public void testPlainText() {
        assertUnchanged("Hello world");
    }

    // --- URL redaction ---

    @Test
    public void testHttpUrl() {
        assertRedacted("Visit " + LINK_REMOVED + " for details", "Visit http://example.com for details");
    }

    @Test
    public void testHttpsUrl() {
        assertRedacted("Check " + LINK_REMOVED, "Check https://example.com/path?q=1");
    }

    @Test
    public void testHttpsUrlWithPort() {
        assertRedacted(LINK_REMOVED, "https://example.com:8080/path");
    }

    @Test
    public void testHttpUrlCaseInsensitive() {
        assertRedacted(LINK_REMOVED, "HTTP://EXAMPLE.COM");
    }

    @Test
    public void testFtpUrl() {
        assertRedacted("Download from " + LINK_REMOVED, "Download from ftp://files.example.com/pub");
    }

    @Test
    public void testBareWwwUrl() {
        assertRedacted("Go to " + LINK_REMOVED, "Go to www.example.com");
    }

    @Test
    public void testBareWwwUrlCaseInsensitive() {
        assertRedacted(LINK_REMOVED, "WWW.EXAMPLE.COM");
    }

    @Test
    public void testMultipleUrls() {
        assertRedacted("See " + LINK_REMOVED + " and " + LINK_REMOVED, "See https://one.com and http://two.com/page");
    }

    @Test
    public void testUrlWithFragment() {
        assertRedacted(LINK_REMOVED, "https://example.com/page#section");
    }

    // --- Email redaction ---

    @Test
    public void testSimpleEmail() {
        assertRedacted("Contact " + EMAIL_REMOVED, "Contact user@example.com");
    }

    @Test
    public void testEmailWithPlus() {
        assertRedacted(EMAIL_REMOVED, "first.last+tag@sub.domain.co.uk");
    }

    @Test
    public void testEmailWithDots() {
        assertRedacted("Send to " + EMAIL_REMOVED + " please", "Send to john.doe@company.org please");
    }

    @Test
    public void testMultipleEmails() {
        assertRedacted(EMAIL_REMOVED + " and " + EMAIL_REMOVED, "a@b.com and c@d.org");
    }

    // --- Mixed content ---

    @Test
    public void testMixedUrlAndEmail() {
        assertRedacted("Visit " + LINK_REMOVED + " or email " + EMAIL_REMOVED,
                "Visit https://example.com or email admin@example.com");
    }

    @Test
    public void testMultilineContent() {
        assertRedacted("First line\n" + LINK_REMOVED + "\n" + EMAIL_REMOVED + "\nLast line",
                "First line\nhttps://evil.com\nuser@evil.com\nLast line");
    }

    @Test
    public void testUrlOnly() {
        assertRedacted(LINK_REMOVED, "https://evil.com/phishing");
    }

    @Test
    public void testSpecialCharactersPreserved() {
        assertUnchanged("Use <b>bold</b> & \"quotes\" are fine!");
    }

    @Test
    public void testNewlinesAndWhitespacePreserved() {
        assertUnchanged("Line one\n  Line two\n\tLine three");
    }
}
