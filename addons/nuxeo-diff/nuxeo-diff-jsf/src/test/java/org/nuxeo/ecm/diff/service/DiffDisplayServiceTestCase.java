/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.diff.model.DiffDisplayBlock;
import org.nuxeo.ecm.diff.model.PropertyDiffDisplay;

/**
 * Super class for diff display service test cases.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class DiffDisplayServiceTestCase {

    /**
     * Checks a diff display block.
     *
     * @param diffDisplayBlock the diff display block
     * @param label the label
     * @param schemaCount the schema count
     */
    protected boolean checkDiffDisplayBlock(DiffDisplayBlock diffDisplayBlock, String label, int schemaCount) {

        // Check label
        assertEquals(label, diffDisplayBlock.getLabel());

        // Check schema count on left value
        Map<String, Map<String, PropertyDiffDisplay>> value = diffDisplayBlock.getLeftValue();
        assertNotNull(value);
        assertEquals(schemaCount, value.size());

        // Check schema count on right value
        value = diffDisplayBlock.getRightValue();
        assertNotNull(value);
        assertEquals(schemaCount, value.size());

        // TODO: manage contentDiff

        return true;
    }

    /**
     * Checks a diff display block schema.
     *
     * @param diffDisplayBlock the diff display block
     * @param schemaName the schema name
     * @param fieldCount the field count
     * @param fieldNames the field names
     */
    protected void checkDiffDisplayBlockSchema(DiffDisplayBlock diffDisplayBlock, String schemaName, int fieldCount,
            List<String> fieldNames) {

        // Check fields on left value
        Map<String, PropertyDiffDisplay> fields = diffDisplayBlock.getLeftValue().get(schemaName);
        assertNotNull(fields);
        assertEquals(fieldCount, fields.size());
        for (String fieldName : fieldNames) {
            assertTrue(fields.containsKey(fieldName));
        }

        // Check fields on right value
        fields = diffDisplayBlock.getRightValue().get(schemaName);
        assertNotNull(fields);
        assertEquals(fieldCount, fields.size());
        for (String fieldName : fieldNames) {
            assertTrue(fields.containsKey(fieldName));
        }

        // TODO: manage contentDiff
    }

}
