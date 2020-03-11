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

public class TestNotNullConstraint {

    @Test
    public void testNotNullConstraintSupportedType() {
        assertTrue(DoubleType.INSTANCE.support(NotNullConstraint.class));
        assertTrue(IntegerType.INSTANCE.support(NotNullConstraint.class));
        assertTrue(LongType.INSTANCE.support(NotNullConstraint.class));
        assertTrue(StringType.INSTANCE.support(NotNullConstraint.class));
        assertTrue(DateType.INSTANCE.support(NotNullConstraint.class));
        assertTrue(BinaryType.INSTANCE.support(NotNullConstraint.class));
        assertTrue(BooleanType.INSTANCE.support(NotNullConstraint.class));
    }

    @Test
    public void testNotNullConstraintNominals() {
        assertTrue(NotNullConstraint.get().validate(123));
        assertTrue(NotNullConstraint.get().validate("123"));
        assertTrue(NotNullConstraint.get().validate(NotNullConstraint.get()));
        assertFalse(NotNullConstraint.get().validate(null));
    }

    @Test
    public void testNotNullConstraintDescription() {
        Constraint.Description description = NotNullConstraint.get().getDescription();
        assertEquals("NotNullConstraint", description.getName());
        Map<String, Serializable> params = description.getParameters();
        assertEquals(0, params.size());
    }

}
