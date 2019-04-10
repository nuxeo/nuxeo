/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.suggestbox.service.suggesters;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple interpolation for i18n messages with parameters independently of the UI layer. Only support parameter
 * replacement with the "{0}", "{1}"... syntax.
 *
 * @author ogrisel
 */
public class I18nHelper {

    public static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)\\}");

    protected Map<String, String> messages;

    public I18nHelper(Map<String, String> messages) {
        this.messages = messages;
    }

    public static I18nHelper instanceFor(Map<String, String> messages) {
        return new I18nHelper(messages);
    }

    public static String interpolate(String message, Object... params) {
        Matcher matcher = PLACEHOLDER.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            int paramId = Integer.valueOf(matcher.group(1));
            if (paramId >= 0 && paramId < params.length) {
                String replacement = params[paramId].toString();
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                throw new IllegalArgumentException(String.format("Invalid placeholder %d in message '%s': %d "
                        + "parameters provided", paramId, message, params.length));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public String translate(String label, Object... params) {
        String message = messages.get(label);
        if (message == null) {
            message = label;
        }
        return interpolate(message, params);
    }

}
