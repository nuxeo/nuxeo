/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu, Ronan DANIELLOU <rdaniellou@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

import com.google.common.base.Objects;

/**
 * Inline properties file content. This class exists to have a real type for parameters accepting properties content.
 *
 * @see Constants
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Properties extends HashMap<String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Spaces may be legitimate part of the value, there is no reason to trim them. But before NXP-19050, the behavior
     * was to trim the values. We have put in place a contribution, which is overridden before Nuxeo 8 series, for
     * backward compatibility. See NXP-19170.
     *
     * @since 8.2
     */
    public static final String IS_PROPERTY_VALUE_TRIMMED_KEY = "nuxeo.automation.properties.value.trim";

    /**
     * Default value is <code>false</code>.
     *
     * @since 8.2
     */
    protected static boolean isPropertyValueTrimmed() {
        return Framework.getService(ConfigurationService.class).isBooleanPropertyTrue(IS_PROPERTY_VALUE_TRIMMED_KEY);
    }

    public static final String PROPERTIES_MULTILINE_ESCAPE = "nuxeo" + ".automation.properties.multiline.escape";

    protected static final String multiLineEscape = Objects.firstNonNull(
            Framework.getProperty(PROPERTIES_MULTILINE_ESCAPE), "true");

    public Properties() {
    }

    public Properties(int size) {
        super(size);
    }

    public Properties(Map<String, String> props) {
        super(props);
    }

    public Properties(String content) throws IOException {
        StringReader reader = new StringReader(content);
        Map<String,String> props = new HashMap<>();
        loadProperties(reader, props);
        putAll(props);
    }

    /**
     * Constructs a Properties map based on a Json node.
     *
     * @param node
     * @throws IOException
     * @since 5.7.3
     */
    public Properties(JsonNode node) throws IOException {
        Iterator<Entry<String, JsonNode>> fields = node.getFields();
        ObjectMapper om = new ObjectMapper();
        while (fields.hasNext()) {
            Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            JsonNode subNode = entry.getValue();
            put(key, extractValueFromNode(subNode, om));
        }
    }

    /**
     * @param om
     * @param subNode
     * @return
     * @throws IOException
     * @since 5.8-HF01
     */
    private String extractValueFromNode(JsonNode node, ObjectMapper om) throws IOException {
        if (!node.isNull()) {
            return node.isContainerNode() ? om.writeValueAsString(node) : node.getValueAsText();
        } else {
            return null;
        }
    }

    public static Map<String, String> loadProperties(Reader reader) throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        loadProperties(reader, map);
        return map;
    }

    public static void loadProperties(Reader reader, Map<String, String> map) throws IOException {

        boolean isPropertyValueToBeTrimmed = isPropertyValueTrimmed();
        BufferedReader in = new BufferedReader(reader);
        String line = in.readLine();
        String prevLine = null;
        String lineSeparator = "\n";
        while (line != null) {
            if (prevLine == null) {
                // we start a new property
                if (line.startsWith("#") || StringUtils.isBlank(line)) {
                    // skip comments, empty or blank line
                    line = in.readLine();
                    continue;
                }
            }
            if (line.endsWith("\\") && Boolean.valueOf(multiLineEscape)) {
                line = line.substring(0, line.length() - 1);
                prevLine = (prevLine != null ? prevLine + line : line) + lineSeparator;
                line = in.readLine();
                continue;
            }
            if (prevLine != null) {
                line = prevLine + line;
            }
            prevLine = null;
            setPropertyLine(map, line, isPropertyValueToBeTrimmed);
            line = in.readLine();
        }
        if (prevLine != null) {
            setPropertyLine(map, prevLine, isPropertyValueToBeTrimmed);
        }
    }

    /**
     * @param isPropertyValueToBeTrimmed The caller may store the value, to prevent from fetching it every time.
     */
    private static void setPropertyLine(Map<String, String> map, String line, boolean isPropertyValueToBeTrimmed)
            throws IOException {
        int i = line.indexOf('=');
        if (i == -1) {
            throw new IOException("Invalid property line (cannot find a '=') in: '" + line + "'");
        }
        // we trim() the key, but not the value (by default, but you may override this for backward compatibility with
        // former code. See: NXP-19170): spaces and new lines are legitimate part of the value
        String value = line.substring(i + 1);
        if (isPropertyValueToBeTrimmed) {
            value = value.trim();
        }
        map.put(line.substring(0, i).trim(), value);
    }

}
