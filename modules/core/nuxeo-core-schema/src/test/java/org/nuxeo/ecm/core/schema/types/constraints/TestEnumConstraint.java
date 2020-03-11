/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.schema.types.constraints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;

public class TestEnumConstraint {

    private EnumConstraint simple = new EnumConstraint(Arrays.asList("one", "two", "three"));

    @Test
    public void testEnumConstraintSupportedType() {
        assertTrue(DoubleType.INSTANCE.support(EnumConstraint.class));
        assertTrue(IntegerType.INSTANCE.support(EnumConstraint.class));
        assertTrue(LongType.INSTANCE.support(EnumConstraint.class));
        assertTrue(StringType.INSTANCE.support(EnumConstraint.class));
        assertFalse(DateType.INSTANCE.support(EnumConstraint.class));
        assertFalse(BinaryType.INSTANCE.support(EnumConstraint.class));
        assertFalse(BooleanType.INSTANCE.support(EnumConstraint.class));
    }

    @Test
    public void testEnumConstraintNominals() {
        assertTrue(simple.validate("one"));
        assertTrue(simple.validate("two"));
        assertTrue(simple.validate("three"));
        assertFalse(simple.validate("paff"));
        assertFalse(simple.validate("pshitt"));
        assertFalse(simple.validate("pong"));
    }

    @Test
    public void testEnumConstraintNullIsOk() {
        assertTrue(simple.validate(null));
    }

    @Test
    public void testEnumConstraintHandleString() {
        assertTrue(simple.validate("one"));
        assertFalse(simple.validate("pshittt"));
    }

    @Test
    public void testPatternConstraintHandleAnyObject() {
        assertTrue(simple.validate(new Object() {
            @Override
            public String toString() {
                return "one";
            }
        }));
        assertFalse(simple.validate(new Object() {
            @Override
            public String toString() {
                return "pshittt";
            }
        }));
    }

    @Test
    public void testEnumConstraintEmptyEnum() {
        assertFalse(new EnumConstraint(new ArrayList<String>()).validate("one"));
    }

    @Test
    public void testEnumConstraintDescription() {
        Constraint.Description description = simple.getDescription();
        assertEquals("EnumConstraint", description.getName());
        Map<String, Serializable> params = description.getParameters();
        assertEquals(1, params.size());
        assertTrue(params.containsKey("Values"));
        Serializable values = params.get("Values");
        assertTrue(values instanceof Collection);
        assertTrue(((Collection<?>) values).contains("one"));
        assertTrue(((Collection<?>) values).contains("two"));
        assertTrue(((Collection<?>) values).contains("three"));
    }

}
