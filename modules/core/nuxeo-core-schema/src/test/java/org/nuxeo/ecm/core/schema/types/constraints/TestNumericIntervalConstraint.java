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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;

public class TestNumericIntervalConstraint {

    private static final int VAL1 = 1;

    private static final int VAL2 = 2;

    private static final int VAL3 = 3;

    private static final int VAL4 = 4;

    private static final int VAL5 = 5;

    private static final String BAD = "pshitt";

    private NumericIntervalConstraint minInMaxEx = new NumericIntervalConstraint(VAL2, true, VAL4, false);

    private NumericIntervalConstraint minIn = new NumericIntervalConstraint(VAL2, true, null, false);

    @Test
    public void testNumericIntervalConstraintSupportedType() {
        assertTrue(DoubleType.INSTANCE.support(NumericIntervalConstraint.class));
        assertTrue(IntegerType.INSTANCE.support(NumericIntervalConstraint.class));
        assertTrue(LongType.INSTANCE.support(NumericIntervalConstraint.class));
        assertFalse(DateType.INSTANCE.support(NumericIntervalConstraint.class));
        assertFalse(BinaryType.INSTANCE.support(NumericIntervalConstraint.class));
        assertFalse(BooleanType.INSTANCE.support(NumericIntervalConstraint.class));
        assertFalse(StringType.INSTANCE.support(NumericIntervalConstraint.class));
    }

    @Test
    public void testNumericIntervalConstraintNominals() {
        NumericIntervalConstraint minInMaxIn, minExMaxIn, minInMaxEx, minExMaxEx, minIn, minEx, maxIn, maxEx, any, point, failExPoint, failInverted, failMax, failMin;
        minInMaxIn = new NumericIntervalConstraint(VAL2, true, VAL4, true);
        genericTest(minInMaxIn, false, true, true, true, false);
        minExMaxIn = new NumericIntervalConstraint(VAL2, false, VAL4, true);
        genericTest(minExMaxIn, false, false, true, true, false);
        minInMaxEx = new NumericIntervalConstraint(VAL2, true, VAL4, false);
        genericTest(minInMaxEx, false, true, true, false, false);
        minExMaxEx = new NumericIntervalConstraint(VAL2, false, VAL4, false);
        genericTest(minExMaxEx, false, false, true, false, false);
        minIn = new NumericIntervalConstraint(VAL2, true, null, false);
        genericTest(minIn, false, true, true, true, true);
        minEx = new NumericIntervalConstraint(VAL2, false, null, false);
        genericTest(minEx, false, false, true, true, true);
        maxIn = new NumericIntervalConstraint(null, false, VAL4, true);
        genericTest(maxIn, true, true, true, true, false);
        maxEx = new NumericIntervalConstraint(null, false, VAL4, false);
        genericTest(maxEx, true, true, true, false, false);
        any = new NumericIntervalConstraint(null, true, null, true);
        genericTest(any, true, true, true, true, true);
        point = new NumericIntervalConstraint(VAL2, true, VAL2, true);
        genericTest(point, false, true, false, false, false);
        failExPoint = new NumericIntervalConstraint(VAL2, false, VAL2, false);
        genericTest(failExPoint, false, false, false, false, false);
        failInverted = new NumericIntervalConstraint(VAL4, false, VAL2, false);
        genericTest(failInverted, false, false, false, false, false);
        failMax = new NumericIntervalConstraint(VAL2, true, BAD, false);
        genericTest(failMax, false, true, true, true, true);
        failMin = new NumericIntervalConstraint(BAD, false, VAL4, true);
        genericTest(failMin, true, true, true, true, false);
    }

    private void genericTest(NumericIntervalConstraint constraint, boolean less, boolean lowerBound, boolean inner,
            boolean upperBound, boolean greater) {
        assertEquals(less, constraint.validate(VAL1));
        assertEquals(lowerBound, constraint.validate(VAL2));
        assertEquals(inner, constraint.validate(VAL3));
        assertEquals(upperBound, constraint.validate(VAL4));
        assertEquals(greater, constraint.validate(VAL5));
    }

    @Test
    public void testNumericIntervalConstraintNullIsOk() {
        assertTrue(minInMaxEx.validate(null));
    }

    @Test
    public void testNumericIntervalConstraintHandleString() {
        assertTrue(minInMaxEx.validate("3"));
        assertFalse(minInMaxEx.validate("5"));
    }

    @Test
    public void testNumericIntervalConstraintHandleNumerics() {
        assertTrue(minInMaxEx.validate(new AtomicInteger(3)));
        assertFalse(minInMaxEx.validate(new AtomicInteger(5)));
        assertTrue(minInMaxEx.validate(new AtomicLong(3l)));
        assertFalse(minInMaxEx.validate(new AtomicLong(5l)));
        assertTrue(minInMaxEx.validate(new BigDecimal("3")));
        assertFalse(minInMaxEx.validate(new BigDecimal("5")));
        assertTrue(minInMaxEx.validate(new BigInteger("3")));
        assertFalse(minInMaxEx.validate(new BigInteger("5")));
        assertTrue(minInMaxEx.validate(Byte.valueOf("3")));
        assertFalse(minInMaxEx.validate(Byte.valueOf("5")));
        assertTrue(minInMaxEx.validate(Double.valueOf("3")));
        assertFalse(minInMaxEx.validate(Double.valueOf("5")));
        assertTrue(minInMaxEx.validate(Float.valueOf("3")));
        assertFalse(minInMaxEx.validate(Float.valueOf("5")));
        assertTrue(minInMaxEx.validate(Integer.valueOf("3")));
        assertFalse(minInMaxEx.validate(Integer.valueOf("5")));
        assertTrue(minInMaxEx.validate(Long.valueOf("3")));
        assertFalse(minInMaxEx.validate(Long.valueOf("5")));
        assertTrue(minInMaxEx.validate(Short.valueOf("3")));
        assertFalse(minInMaxEx.validate(Short.valueOf("5")));
    }

    @Test
    public void testNumericIntervalConstraintHandlePrimitiveNumerics() {
        assertTrue(minInMaxEx.validate(Byte.valueOf("3").byteValue()));
        assertFalse(minInMaxEx.validate(Byte.valueOf("5").byteValue()));
        assertTrue(minInMaxEx.validate(Double.valueOf("3").doubleValue()));
        assertFalse(minInMaxEx.validate(Double.valueOf("5").doubleValue()));
        assertTrue(minInMaxEx.validate(Float.valueOf("3").floatValue()));
        assertFalse(minInMaxEx.validate(Float.valueOf("5").floatValue()));
        assertTrue(minInMaxEx.validate(Integer.valueOf("3").intValue()));
        assertFalse(minInMaxEx.validate(Integer.valueOf("5").intValue()));
        assertTrue(minInMaxEx.validate(Long.valueOf("3").longValue()));
        assertFalse(minInMaxEx.validate(Long.valueOf("5").longValue()));
        assertTrue(minInMaxEx.validate(Short.valueOf("3").shortValue()));
        assertFalse(minInMaxEx.validate(Short.valueOf("5").shortValue()));
    }

    @Test
    public void testNumericIntervalConstraintDescription() {
        Constraint.Description description = minInMaxEx.getDescription();
        assertEquals("NumericIntervalConstraint", description.getName());
        Map<String, Serializable> params = description.getParameters();
        assertEquals(4, params.size());
        assertTrue(params.containsKey("Minimum"));
        assertTrue(params.containsKey("MinimumInclusive"));
        assertTrue(params.containsKey("Maximum"));
        assertTrue(params.containsKey("MaximumInclusive"));
        assertEquals(new BigDecimal(VAL2), params.get("Minimum"));
        assertTrue((Boolean) params.get("MinimumInclusive"));
        assertEquals(new BigDecimal(VAL4), params.get("Maximum"));
        assertFalse((Boolean) params.get("MaximumInclusive"));
    }

    @Test
    public void testNumericIntervalConstraintDescriptionOneBound() {
        Map<String, Serializable> params = minIn.getDescription().getParameters();
        assertEquals(2, params.size());
        assertTrue(params.containsKey("Minimum"));
        assertTrue(params.containsKey("MinimumInclusive"));
        assertEquals(new BigDecimal(VAL2), params.get("Minimum"));
        assertTrue((Boolean) params.get("MinimumInclusive"));
    }

}
