/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 2023.13
 */
public final class JsonNodeHelper {

    private JsonNodeHelper() {
        // Helper class
    }

    public static List<JsonNode> getEntries(JsonNode node) {
        return getEntries("documents", node);
    }

    public static List<JsonNode> getEntries(String expectedEntityType, JsonNode node) {
        assertEquals(expectedEntityType, node.get("entity-type").asText());
        assertTrue(node.get("entries").isArray());
        List<JsonNode> result = new ArrayList<>();
        Iterator<JsonNode> elements = node.get("entries").elements();
        while (elements.hasNext()) {
            result.add(elements.next());
        }
        return result;
    }

    public static int getEntriesSize(JsonNode node) {
        return getEntries(node).size();
    }

    public static int getEntriesSize(String expectedEntityType, JsonNode node) {
        return getEntries(expectedEntityType, node).size();
    }

    public static String getErrorMessage(JsonNode node) {
        assertTrue(hasErrorMessage(node));
        assertTrue("Exception message is not present in response", node.has("message"));
        assertTrue("Exception message is not textual", node.get("message").isTextual());
        return node.get("message").asText();
    }

    /**
     * @since 2025.9
     */
    public static int getErrorStatus(JsonNode node) {
        assertTrue(hasErrorMessage(node));
        assertTrue("Exception status is not present in response", node.has("status"));
        assertTrue("Exception status is not number", node.get("status").isNumber());
        return node.get("status").asInt();
    }

    public static boolean hasErrorMessage(JsonNode node) {
        return node.get("entity-type").asText().equals("exception");
    }
}
