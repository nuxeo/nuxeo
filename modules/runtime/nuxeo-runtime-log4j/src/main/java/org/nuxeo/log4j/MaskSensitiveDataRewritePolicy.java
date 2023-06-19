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

package org.nuxeo.log4j;

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

    protected Redactor redactor = new Redactor();

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
        return redactor.maskSensitive(msg);
    }

    @PluginFactory
    public static MaskSensitiveDataRewritePolicy createPolicy() {
        return new MaskSensitiveDataRewritePolicy();
    }
}
