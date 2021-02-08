/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.log4j2;

import java.util.regex.Pattern;

import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;

/**
 * @since 11.5
 */
@Plugin(name = "MaskSensitiveData", category = Core.CATEGORY_NAME, elementType = "rewritePolicy")
public final class MaskSensitiveDataRewritePolicy implements RewritePolicy {

    // pattern taken from https://github.com/awslabs/git-secrets
    protected static final Pattern AWS_KEY_PATTERN = Pattern.compile(
            "(A3T[A-Z0-9]|AKIA|AGPA|AIDA|AROA|AIPA|ANPA|ANVA|ASIA)([A-Z0-9]{3})[A-Z0-9]{9}([A-Z0-9]{4})");

    protected static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("([0-9]{4})[0-9]{0,2}([- ]?[0-9]{3,6}){3,5}");

    protected static final Pattern GCP_KEY_PATTERN = Pattern.compile(
            "(AIza[0-9A-Za-z-_]{3})[0-9A-Za-z-_]{28}([0-9A-Za-z-_]{4})");

    @Override
    public LogEvent rewrite(LogEvent source) {
        Message msg = source.getMessage();
        if (msg == null) {
            return source;
        }
        String formattedMessage = msg.getFormattedMessage();
        return new Log4jLogEvent.Builder(source).setMessage(new SimpleMessage(maskSensitive(formattedMessage))).build();
    }

    protected String maskSensitive(String msg) {
        var awsMatcher = AWS_KEY_PATTERN.matcher(msg);
        if (awsMatcher.find()) {
            msg = awsMatcher.replaceAll("$1$2-AWS_KEY-$3");
        }
        var ccMatcher = CREDIT_CARD_PATTERN.matcher(msg);
        if (ccMatcher.find()) {
            String card = ccMatcher.group().replaceAll("[- ]", "");
            if (isValidCreditCard(card)) {
                msg = ccMatcher.replaceAll("$1-CRED-CARD-XXXX");
            }
        }
        var gcpMatcher = GCP_KEY_PATTERN.matcher(msg);
        if (gcpMatcher.find()) {
            msg = gcpMatcher.replaceAll("$1-GCP_KEY-$2");
        }
        return msg;
    }

    protected boolean isValidCreditCard(String card) {
        // Luhn Algorithm
        int sum = 0;
        boolean even = false;
        for (var i = card.length() - 1; i >= 0; i--) {
            int digit = card.charAt(i) - '0';
            if (even) {
                digit = 2 * digit;
            }
            // handle the two digits case
            sum += digit / 10;
            sum += digit % 10;
            even = !even;
        }
        return sum % 10  == 0;
    }

    @PluginFactory
    public static MaskSensitiveDataRewritePolicy createPolicy() {
        return new MaskSensitiveDataRewritePolicy();
    }
}
