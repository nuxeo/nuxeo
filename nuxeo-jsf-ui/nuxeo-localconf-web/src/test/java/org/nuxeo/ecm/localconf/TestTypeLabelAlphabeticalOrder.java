/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.localconf;

import static org.junit.Assert.assertTrue;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.localconf.UITypesConfigurationActions.TypeLabelAlphabeticalOrder;
import org.nuxeo.ecm.platform.types.Type;

/**
 * Tests the {@link UITypesConfigurationActions.TypeLabelAlphabeticalOrder} {@link Comparator}.
 *
 * @since 9.2
 */
public class TestTypeLabelAlphabeticalOrder {

    @Test
    public void testCompare() {
        Map<String, String> messages = new HashMap<>();
        messages.put("label.file", "File");
        messages.put("label.note", "A note");
        TypeLabelAlphabeticalOrder comparator = new TypeLabelAlphabeticalOrder(messages);

        Type type1 = new Type();
        Type type2 = new Type();
        type1.setId("File");
        type2.setId("Note");

        // Empty label for both types: fall back on ids -> "File" < "Note"
        assertTrue(comparator.compare(type1, type2) < 0);

        // Empty label for type2: use label for type1 and fall back on id for type2 -> "File" < "Note"
        type1.setLabel("label.file");
        assertTrue(comparator.compare(type1, type2) < 0);

        // Empty label for type1: use label for type2 and fall back on id for type1 -> "File" > "A note"
        type1.setLabel(null);
        type2.setLabel("label.note");
        assertTrue(comparator.compare(type1, type2) > 0);

        // Label for both types -> "File" > "A note"
        type1.setLabel("label.file");
        assertTrue(comparator.compare(type1, type2) > 0);

        // Empty messages: fall back on labels -> "label.file" < "label.note"
        messages.clear();
        assertTrue(comparator.compare(type1, type2) < 0);
    }
}
