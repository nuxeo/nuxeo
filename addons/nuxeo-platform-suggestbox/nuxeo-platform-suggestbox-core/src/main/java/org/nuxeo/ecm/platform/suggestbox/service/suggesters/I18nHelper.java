/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.suggestbox.service.suggesters;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple interpolation for i18n messages with parameters independently of the
 * UI layer.
 *
 * Only support parameter replacement with the "{0}", "{1}"... syntax.
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
                matcher.appendReplacement(sb,
                        Matcher.quoteReplacement(replacement));
            } else {
                throw new IllegalArgumentException(String.format(
                        "Invalid placeholder %d in message '%s': %d "
                                + "parameters provided", paramId, message,
                        params.length));
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
