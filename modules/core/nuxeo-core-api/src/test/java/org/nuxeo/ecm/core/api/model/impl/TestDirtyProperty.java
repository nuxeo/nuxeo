/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat
 */
package org.nuxeo.ecm.core.api.model.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;

public class TestDirtyProperty extends AbstractTestProperty {

    @Test
    public void testScalarPropertyInitNotDirty() {
        ScalarProperty property = getScalarProperty();
        assertFalse(property.isDirty());
    }

    @Test
    public void testScalarPropertyUpdatedDirty() {
        ScalarProperty property = getScalarProperty();
        property.setValue("test");
        assertTrue(property.isDirty());
    }

    @Test
    public void testScalarPropertyNewNotDirty() {
        ScalarProperty property = getScalarProperty();
        property.setValue("test");
        property.clearDirtyFlags();
        assertFalse(property.isDirty());
    }

    @Test
    public void testScalarPropertyChangedDirty() {
        ScalarProperty property = getScalarProperty();
        property.setValue("test1");
        property.clearDirtyFlags();
        property.setValue("test2");
        assertTrue(property.isDirty());
    }

    @Test
    public void testScalarPropertyNullDirty() {
        ScalarProperty property = getScalarProperty();
        property.setValue("test1");
        property.clearDirtyFlags();
        property.setValue(null);
        assertTrue(property.isDirty());
    }

    @Test
    public void testScalarPropertyNullToNullNotDirty() {
        ScalarProperty property = getScalarProperty();
        property.setValue(null);
        property.clearDirtyFlags();
        property.setValue(null);
        assertFalse(property.isDirty());
    }

    @Test
    public void testScalarPropertyRemoveDirty() {
        ScalarProperty property = getScalarProperty();
        property.setValue("test1");
        property.clearDirtyFlags();
        property.remove();
        assertTrue(property.isDirty());
    }

    @Test
    public void testScalarPropertyChangedWithSameValueNotDirty() {
        ScalarProperty property = getScalarProperty();
        property.setValue("test1");
        property.clearDirtyFlags();
        property.setValue("test1");
        assertFalse(property.isDirty());
    }

    @Test
    public void testComplexPropertyInitNotDirty() {
        ComplexProperty property = getComplexProperty();
        assertFalse(property.isDirty());
    }

    @Test
    public void testComplexPropertyUpdatedDirty() {
        ComplexProperty property = getComplexProperty();
        Map<String, String> value = new HashMap<>();
        value.put("test1", "test1");
        value.put("test2", "test2");
        property.setValue(value);
        assertTrue(property.isDirty());
        assertTrue(property.get("test1").isDirty());
        assertTrue(property.get("test2").isDirty());
    }

    @Test
    public void testComplexPropertyNewNotDirty() {
        ComplexProperty property = getComplexProperty();
        Map<String, String> value = new HashMap<>();
        value.put("test1", "test1");
        value.put("test2", "test2");
        property.setValue(value);
        property.clearDirtyFlags();
        assertFalse(property.isDirty());
        assertFalse(property.get("test1").isDirty());
        assertFalse(property.get("test2").isDirty());
    }

    @Test
    public void testComplexPropertyChangedDirty() {
        ComplexProperty property = getComplexProperty();
        Map<String, String> value = new HashMap<>();
        value.put("test1", "test1");
        value.put("test2", "test2");
        property.setValue(value);
        property.clearDirtyFlags();
        Map<String, String> value2 = new HashMap<>();
        value2.put("test1", "test12");
        value2.put("test2", "test22");
        property.setValue(value2);
        assertTrue(property.isDirty());
        assertTrue(property.get("test1").isDirty());
        assertTrue(property.get("test2").isDirty());
    }

    @Test
    public void testComplexPropertyNullDirty() {
        ComplexProperty property = getComplexProperty();
        Map<String, String> value = new HashMap<>();
        value.put("test1", "test1");
        value.put("test2", "test2");
        property.setValue(value);
        property.clearDirtyFlags();
        property.setValue(null);
        assertTrue(property.isDirty());
        assertTrue(property.get("test1").isDirty());
        assertTrue(property.get("test2").isDirty());
    }

    @Test
    public void testComplexPropertyNullToNullNotDirty() {
        ComplexProperty property = getComplexProperty();
        property.setValue(null);
        property.clearDirtyFlags();
        property.setValue(null);
        assertFalse(property.isDirty());
    }

    @Test
    public void testComplexPropertyRemoveDirty() {
        ComplexProperty property = getComplexProperty();
        Map<String, String> value = new HashMap<>();
        value.put("test1", "test1");
        value.put("test2", "test2");
        property.setValue(value);
        property.clearDirtyFlags();
        property.remove();
        assertTrue(property.isDirty());
    }

    @Test
    public void testComplexPropertyChangedWithSameValueStillDirty() {
        ComplexProperty property = getComplexProperty();
        Map<String, String> value = new HashMap<>();
        value.put("test1", "test1");
        value.put("test2", "test2");
        property.setValue(value);
        property.clearDirtyFlags();
        Map<String, String> value2 = new HashMap<>();
        value2.put("test1", "test1");
        value2.put("test2", "test2");
        property.setValue(value2);
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        if (schemaManager.getClearComplexPropertyBeforeSet()) {
            // still dirty because we rewrite everything on setValue
            assertTrue(property.isDirty());
            assertTrue(property.get("test1").isDirty());
            assertTrue(property.get("test2").isDirty());
        } else {
            assertFalse(property.isDirty());
            assertFalse(property.get("test1").isDirty());
            assertFalse(property.get("test2").isDirty());
        }
    }

    @Test
    public void testComplexPropertyPartialChangedStillDirty() {
        ComplexProperty property = getComplexProperty();
        Map<String, String> value = new HashMap<>();
        value.put("test1", "test1");
        value.put("test2", "test2");
        property.setValue(value);
        property.clearDirtyFlags();
        Map<String, String> value2 = new HashMap<>();
        value2.put("test1", "test12");
        value2.put("test2", "test2");
        property.setValue(value2);
        assertTrue(property.isDirty());
        assertTrue(property.get("test1").isDirty());
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        if (schemaManager.getClearComplexPropertyBeforeSet()) {
            assertTrue(property.get("test2").isDirty());
        } else {
            assertFalse(property.get("test2").isDirty());
        }
    }

    @Test
    public void testComplexPropertyAddChildStillDirty() {
        ComplexProperty property = getComplexProperty();
        Map<String, String> value = new HashMap<>();
        value.put("test1", "test1");
        property.setValue(value);
        property.clearDirtyFlags();
        Map<String, String> value2 = new HashMap<>();
        value2.put("test1", "test1");
        value2.put("test2", "test2");
        property.setValue(value2);
        assertTrue(property.isDirty());
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        if (schemaManager.getClearComplexPropertyBeforeSet()) {
            assertTrue(property.get("test1").isDirty());
            assertTrue(property.get("test2").isDirty());
        } else {
            assertFalse(property.get("test1").isDirty());
            assertTrue(property.get("test2").isDirty());
        }
    }

    @Test
    public void testComplexPropertySetNullChildStillDirty() {
        ComplexProperty property = getComplexProperty();
        Map<String, String> value = new HashMap<>();
        value.put("test1", "test1");
        value.put("test2", "test2");
        property.setValue(value);
        property.clearDirtyFlags();
        Map<String, String> value2 = new HashMap<>();
        value2.put("test1", "test1");
        value2.put("test2", null);
        property.setValue(value2);
        assertTrue(property.isDirty());
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        if (schemaManager.getClearComplexPropertyBeforeSet()) {
            assertTrue(property.get("test1").isDirty());
            assertFalse(property.get("test2").isDirty()); // not dirty because null...
        } else {
            assertFalse(property.get("test1").isDirty());
            assertTrue(property.get("test2").isDirty());
        }
    }

    @Test
    public void testComplexPropertyRemoveChildPartialDirty() {
        ComplexProperty property = getComplexProperty();
        Map<String, String> value = new HashMap<>();
        value.put("test1", "test1");
        value.put("test2", "test2");
        property.setValue(value);
        property.clearDirtyFlags();
        property.get("test2").remove();
        assertTrue(property.isDirty());
        assertFalse(property.get("test1").isDirty());
        assertTrue(property.get("test2").isDirty());
    }

    @Test
    public void testListPropertyInitNotDirty() {
        ListProperty property = getListProperty();
        assertFalse(property.isDirty());
    }

    @Test
    public void testListPropertyUpdatedDirty() {
        ListProperty property = getListProperty();
        property.addValue("test1");
        assertTrue(property.isDirty());
        assertTrue(property.get(0).isDirty());
    }

    @Test
    public void testListPropertyNewNotDirty() {
        ListProperty property = getListProperty();
        property.addValue("test1");
        property.clearDirtyFlags();
        assertFalse(property.isDirty());
        assertFalse(property.get(0).isDirty());
    }

    @Test
    public void testListPropertyChangedDirty() {
        ListProperty property = getListProperty();
        property.addValue("test1");
        property.clearDirtyFlags();
        property.setValue(0, "test2");
        assertTrue(property.isDirty());
        assertTrue(property.get(0).isDirty());
    }

    @Test
    public void testListPropertyNullDirty() {
        ListProperty property = getListProperty();
        property.addValue("test1");
        property.clearDirtyFlags();
        property.setValue(null);
        assertTrue(property.isDirty());
    }

    @Test
    public void testListPropertyNullToNullNotDirty() {
        ListProperty property = getListProperty();
        property.setValue(null);
        property.clearDirtyFlags();
        property.setValue(null);
        assertFalse(property.isDirty());
    }

    @Test
    public void testListPropertyRemoveDirty() {
        ListProperty property = getListProperty();
        property.addValue("test1");
        property.clearDirtyFlags();
        property.remove();
        assertTrue(property.isDirty());
    }

    @Test
    public void testListPropertyChangedWithSameValueNotDirty() {
        ListProperty property = getListProperty();
        property.addValue("test1");
        property.clearDirtyFlags();
        property.setValue(0, "test1");
        assertFalse(property.isDirty());
        assertFalse(property.get(0).isDirty());
    }

    @Test
    public void testListPropertyAddValuePartialDirty() {
        ListProperty property = getListProperty();
        property.addValue("test1");
        property.clearDirtyFlags();
        property.addValue("test2");
        assertTrue(property.isDirty());
        assertFalse(property.get(0).isDirty());
        assertTrue(property.get(1).isDirty());
    }

    @Test
    public void testListPropertyRemoveValuePartialDirty() {
        ListProperty property = getListProperty();
        property.addValue("test1");
        property.addValue("test2");
        property.clearDirtyFlags();
        property.remove(1);
        assertTrue(property.isDirty());
        assertFalse(property.get(0).isDirty());
    }

    @Test
    public void testListPropertyReplaceValuePartialDirty() {
        ListProperty property = getListProperty();
        property.addValue("test1");
        property.addValue("test2");
        property.addValue("test3");
        property.clearDirtyFlags();
        property.setValue(1, "test22");
        assertTrue(property.isDirty());
        assertFalse(property.get(0).isDirty());
        assertTrue(property.get(1).isDirty());
        assertFalse(property.get(2).isDirty());
    }

    @Test
    public void testListPropertyMoveIndexPartialDirty() {
        ListProperty property = getListProperty();
        property.addValue("test1");
        property.addValue("test2");
        property.addValue("test3");
        property.addValue("test4");
        property.clearDirtyFlags();
        Property el1 = property.get(2);
        property.moveTo(el1, 1);
        assertTrue(property.isDirty());
        assertFalse(property.get(0).isDirty());
        assertTrue(property.get(1).isDirty());
        assertTrue(property.get(2).isDirty());
        assertTrue(property.get(3).isDirty());
    }

    @Test
    public void testArrayPropertyInitNotDirty() {
        ArrayProperty property = getArrayProperty();
        assertFalse(property.isDirty());
    }

    @Test
    public void testArrayPropertyUpdatedDirty() {
        ArrayProperty property = getArrayProperty();
        property.setValue(new String[] { "test1", "test2" });
        assertTrue(property.isDirty());
        assertTrue(property.isDirty(0));
        assertTrue(property.isDirty(1));
    }

    @Test
    public void testArrayPropertyNewNotDirty() {
        ArrayProperty property = getArrayProperty();
        property.setValue(new String[] { "test1", "test2" });
        property.clearDirtyFlags();
        assertFalse(property.isDirty());
        assertFalse(property.isDirty(0));
        assertFalse(property.isDirty(1));
    }

    @Test
    public void testArrayPropertyChangedDirty() {
        ArrayProperty property = getArrayProperty();
        property.setValue(new String[] { "test1", "test2" });
        property.clearDirtyFlags();
        property.setValue(new String[] { "test3", "test4" });
        assertTrue(property.isDirty());
        assertTrue(property.isDirty(0));
        assertTrue(property.isDirty(1));
    }

    @Test
    public void testArrayPropertyNullDirty() {
        ArrayProperty property = getArrayProperty();
        property.setValue(new String[] { "test1" });
        property.clearDirtyFlags();
        property.setValue(null);
        assertTrue(property.isDirty());
    }

    @Test
    public void testArrayPropertyNullToNullNotDirty() {
        ArrayProperty property = getArrayProperty();
        property.setValue(null);
        property.clearDirtyFlags();
        property.setValue(null);
        assertFalse(property.isDirty());
    }

    @Test
    public void testArrayPropertyRemoveDirty() {
        ArrayProperty property = getArrayProperty();
        property.setValue(new String[] { "test1" });
        property.clearDirtyFlags();
        property.remove();
        assertTrue(property.isDirty());
    }

    @Test
    public void testArrayPropertyChangedWithSameValueNotDirty() {
        ArrayProperty property = getArrayProperty();
        property.setValue(new String[] { "test1", "test2" });
        property.clearDirtyFlags();
        property.setValue(new String[] { "test1", "test2" });
        assertFalse(property.isDirty());
        assertFalse(property.isDirty(0));
        assertFalse(property.isDirty(1));
    }

    @Test
    public void testArrayPropertyAddValuePartialDirty() {
        ArrayProperty property = getArrayProperty();
        property.setValue(new String[] { "test1", "test2" });
        property.clearDirtyFlags();
        property.setValue(new String[] { "test1", "test2", "test3" });
        assertTrue(property.isDirty());
        assertFalse(property.isDirty(0));
        assertFalse(property.isDirty(1));
        assertTrue(property.isDirty(2));
    }

    @Test
    public void testArrayPropertyRemoveValuePartialDirty() {
        ArrayProperty property = getArrayProperty();
        property.setValue(new String[] { "test1", "test2", "test3" });
        property.clearDirtyFlags();
        property.setValue(new String[] { "test1", "test2" });
        assertTrue(property.isDirty());
        assertFalse(property.isDirty(0));
        assertFalse(property.isDirty(1));
    }

    @Test
    public void testArrayPropertyReplaceValuePartialDirty() {
        ArrayProperty property = getArrayProperty();
        property.setValue(new String[] { "test1", "test2", "test3" });
        property.clearDirtyFlags();
        property.setValue(new String[] { "test1", "test4", "test3" });
        assertTrue(property.isDirty());
        assertFalse(property.isDirty(0));
        assertTrue(property.isDirty(1));
        assertFalse(property.isDirty(2));
    }

}
