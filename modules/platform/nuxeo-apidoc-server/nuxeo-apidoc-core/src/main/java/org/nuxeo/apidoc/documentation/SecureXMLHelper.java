/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.documentation;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Helper for XML secure content management.
 *
 * @since 11.2
 */
public class SecureXMLHelper {

    protected static final String KEYWORDS_PROPERTY = "org.nuxeo.apidoc.secure.xml.keywords";

    public static final List<String> DEFAULT_KEYWORDS = List.of("password", "Password", "secret", "apiKey");

    protected static final String WHITELISTED_KEYWORDS_PROPERTY = "org.nuxeo.apidoc.secure.xml.keywords.whitelisted";

    public static final List<String> DEFAULT_WHITELISTED_KEYWORDS = List.of("passwordField", "passwordHashAlgorithm");

    protected static final String SECRET_VALUE = "********";

    protected static List<String> getKeywordList(String property, List<String> defaultValue) {
        return Framework.getService(ConfigurationService.class)
                        .getString(property)
                        .map(v -> v.split("\\s*,[,\\s]*"))
                        .map(List::of)
                        .orElse(defaultValue);
    }

    /**
     * Makes sure no passwords are embedded in the XML.
     */
    public static String secure(String xml) {
        if (StringUtils.isBlank(xml)) {
            return xml;
        }
        String res = xml;
        List<String> keywords = getKeywordList(KEYWORDS_PROPERTY, DEFAULT_KEYWORDS);
        List<String> whitelist = getKeywordList(WHITELISTED_KEYWORDS_PROPERTY, DEFAULT_WHITELISTED_KEYWORDS);
        for (String kw : keywords) {
            if (res.contains(kw)) {
                for (String pattern : List.of(
                        // node startswith
                        String.format("(?<start><(?<key>\\w*%s)\\s*>)[^<]*(?<end></\\w*%s>)", kw, kw),
                        // node endswith
                        String.format("(?<start><(?<key>%s\\w*)\\s*>)[^<]*(?<end></%s\\w*>)", kw, kw),
                        // attributes startswith
                        String.format("(?<start>(?<key>\\w*%s)=\")[^\"]*(?<end>\")", kw),
                        String.format("(?<start>(?<key>\\w*%s)\"\\s*>)[^<]*(?<end><)", kw),
                        // attributes endswith
                        String.format("(?<start>(?<key>%s\\w*)=\")[^\"]*(?<end>\")", kw),
                        String.format("(?<start>(?<key>%s\\w*)\"\\s*>)[^<]*(?<end><)", kw))) {
                    StringBuffer result = new StringBuffer();
                    Matcher m = Pattern.compile(pattern).matcher(res);
                    while (m.find()) {
                        String replacement;
                        if (whitelist.contains(m.group("key"))) {
                            replacement = m.group();
                        } else {
                            replacement = m.group("start") + SECRET_VALUE + m.group("end");
                        }
                        m.appendReplacement(result, replacement);
                    }
                    res = m.appendTail(result).toString();
                }
            }
        }
        return res;
    }

}
