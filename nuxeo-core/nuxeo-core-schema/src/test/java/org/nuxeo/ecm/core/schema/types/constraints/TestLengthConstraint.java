/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

public class TestLengthConstraint {

    private static final Long MIN = 5l;

    private static final Long MAX = 10l;

    private static final String LESS = "1234";

    private static final String LOWER = "12345";

    private static final String INNER = "1234567";

    private static final String UPPER = "1234567890";

    private static final String GREATER = "1234567890+";

    private static final String BAD = "pshittt";

    LengthConstraint minMax = new LengthConstraint(MIN, MAX);

    LengthConstraint min = new LengthConstraint(MIN, null);

    @Test
    public void testLengthConstraintSupportedType() {
        assertTrue(StringType.INSTANCE.support(LengthConstraint.class));
        assertFalse(DoubleType.INSTANCE.support(LengthConstraint.class));
        assertFalse(IntegerType.INSTANCE.support(LengthConstraint.class));
        assertFalse(LongType.INSTANCE.support(LengthConstraint.class));
        assertFalse(DateType.INSTANCE.support(LengthConstraint.class));
        assertFalse(BinaryType.INSTANCE.support(LengthConstraint.class));
        assertFalse(BooleanType.INSTANCE.support(LengthConstraint.class));
    }

    @Test
    public void testLengthConstraintNominals() {
        LengthConstraint minMax, min, max, any, point, failInverted, failMax, failMin;
        minMax = new LengthConstraint(MIN, MAX);
        genericTest(minMax, false, true, true, true, false);
        min = new LengthConstraint(MIN, null);
        genericTest(min, false, true, true, true, true);
        max = new LengthConstraint(null, MAX);
        genericTest(max, true, true, true, true, false);
        any = new LengthConstraint(null, null);
        genericTest(any, true, true, true, true, true);
        point = new LengthConstraint(MIN, MIN);
        genericTest(point, false, true, false, false, false);
        failInverted = new LengthConstraint(MAX, MIN);
        genericTest(failInverted, false, false, false, false, false);
        failMax = new LengthConstraint(MIN, BAD);
        genericTest(failMax, false, true, true, true, true);
        failMin = new LengthConstraint(BAD, MAX);
        genericTest(failMin, true, true, true, true, false);
    }

    private void genericTest(LengthConstraint constraint, boolean less, boolean lowerBound, boolean inner,
            boolean upperBound, boolean greater) {
        assertEquals(less, constraint.validate(LESS));
        assertEquals(lowerBound, constraint.validate(LOWER));
        assertEquals(inner, constraint.validate(INNER));
        assertEquals(upperBound, constraint.validate(UPPER));
        assertEquals(greater, constraint.validate(GREATER));
    }

    @Test
    public void testLengthConstraintNullIsOk() {
        assertTrue(minMax.validate(null));
    }

    @Test
    public void testLengthConstraintHandleString() {
        assertTrue(minMax.validate(INNER));
        assertFalse(minMax.validate(GREATER));
    }

    @Test
    public void testLengthConstraintHandleAnyObject() {
        assertTrue(minMax.validate(new Object() {
            @Override
            public String toString() {
                return INNER;
            }
        }));
        assertFalse(minMax.validate(new Object() {
            @Override
            public String toString() {
                return GREATER;
            }
        }));
    }

    @Test
    public void testLengthConstraintDescription() {
        Constraint.Description description = minMax.getDescription();
        assertEquals("LengthConstraint", description.getName());
        Map<String, Serializable> params = description.getParameters();
        assertEquals(2, params.size());
        assertTrue(params.containsKey("Minimum"));
        assertTrue(params.containsKey("Maximum"));
        assertEquals(MIN, params.get("Minimum"));
        assertEquals(MAX, params.get("Maximum"));
    }

    @Test
    public void testLengthConstraintDescriptionOneBound() {
        Map<String, Serializable> params = min.getDescription().getParameters();
        assertEquals(1, params.size());
        assertTrue(params.containsKey("Minimum"));
        assertEquals(MIN, params.get("Minimum"));
    }

}
