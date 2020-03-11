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
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;

public class TestPatternConstraint {

    private static final String PATTERN = "[0-9]{4}";

    private static final String YES = "1234";

    private static final String NO = "12345";

    PatternConstraint simple = new PatternConstraint(PATTERN);

    @Test
    public void testPatternConstraintSupportedType() {
        assertTrue(DoubleType.INSTANCE.support(PatternConstraint.class));
        assertTrue(IntegerType.INSTANCE.support(PatternConstraint.class));
        assertTrue(LongType.INSTANCE.support(PatternConstraint.class));
        assertTrue(StringType.INSTANCE.support(PatternConstraint.class));
        assertFalse(DateType.INSTANCE.support(PatternConstraint.class));
        assertFalse(BinaryType.INSTANCE.support(PatternConstraint.class));
        assertFalse(BooleanType.INSTANCE.support(PatternConstraint.class));
    }

    @Test
    public void testPatternConstraintNominals() {
        assertTrue(simple.validate(YES));
        assertFalse(simple.validate(NO));
    }

    @Test
    public void testPatternConstraintNullIsOk() {
        assertTrue(simple.validate(null));
    }

    @Test
    public void testPatternConstraintHandleString() {
        assertTrue(simple.validate(YES));
        assertFalse(simple.validate(NO));
    }

    @Test
    public void testPatternConstraintHandleAnyObject() {
        assertTrue(simple.validate(new Object() {
            @Override
            public String toString() {
                return "1234";
            }
        }));
        assertFalse(simple.validate(new Object() {
            @Override
            public String toString() {
                return "12345";
            }
        }));
    }

    @Test
    public void testPatternConstraintDescription() {
        Constraint.Description description = simple.getDescription();
        assertEquals("PatternConstraint", description.getName());
        Map<String, Serializable> params = description.getParameters();
        assertEquals(1, params.size());
        assertTrue(params.containsKey("Pattern"));
        assertEquals(PATTERN, params.get("Pattern"));
    }

}
