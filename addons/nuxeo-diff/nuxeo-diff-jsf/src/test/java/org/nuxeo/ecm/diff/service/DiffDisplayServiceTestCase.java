/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
    protected boolean checkDiffDisplayBlock(DiffDisplayBlock diffDisplayBlock,
            String label, int schemaCount) {

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
    protected void checkDiffDisplayBlockSchema(
            DiffDisplayBlock diffDisplayBlock, String schemaName,
            int fieldCount, List<String> fieldNames) {

        // Check fields on left value
        Map<String, PropertyDiffDisplay> fields = diffDisplayBlock.getLeftValue().get(
                schemaName);
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
