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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;

public class TestDateIntervalConstraint {

    private static final String NOV1 = "2014-11-01";

    private static final String NOV2 = "2014-11-02";

    private static final String NOV3 = "2014-11-03";

    private static final String NOV4 = "2014-11-04";

    private static final String NOV5 = "2015-11-05";

    private static final String BAD = "2015pshitt-11paf-05";

    private DateIntervalConstraint minInMaxEx = new DateIntervalConstraint(NOV2, true, NOV4, false);

    private DateIntervalConstraint minIn = new DateIntervalConstraint(NOV2, true, null, false);

    @Test
    public void testDateIntervalConstraintSupportedType() {
        assertTrue(DateType.INSTANCE.support(DateIntervalConstraint.class));
        assertFalse(BinaryType.INSTANCE.support(DateIntervalConstraint.class));
        assertFalse(BooleanType.INSTANCE.support(DateIntervalConstraint.class));
        assertFalse(DoubleType.INSTANCE.support(DateIntervalConstraint.class));
        assertFalse(IntegerType.INSTANCE.support(DateIntervalConstraint.class));
        assertFalse(LongType.INSTANCE.support(DateIntervalConstraint.class));
        assertFalse(StringType.INSTANCE.support(DateIntervalConstraint.class));
    }

    @Test
    public void testDateIntervalConstraintNominals() {
        DateIntervalConstraint minInMaxIn, minExMaxIn, minInMaxEx, minExMaxEx, minIn, minEx, maxIn, maxEx, any, point, failExPoint, failInverted, failMax, failMin;
        minInMaxIn = new DateIntervalConstraint(NOV2, true, NOV4, true);
        genericTest(minInMaxIn, false, true, true, true, false);
        minExMaxIn = new DateIntervalConstraint(NOV2, false, NOV4, true);
        genericTest(minExMaxIn, false, false, true, true, false);
        minInMaxEx = new DateIntervalConstraint(NOV2, true, NOV4, false);
        genericTest(minInMaxEx, false, true, true, false, false);
        minExMaxEx = new DateIntervalConstraint(NOV2, false, NOV4, false);
        genericTest(minExMaxEx, false, false, true, false, false);
        minIn = new DateIntervalConstraint(NOV2, true, null, false);
        genericTest(minIn, false, true, true, true, true);
        minEx = new DateIntervalConstraint(NOV2, false, null, false);
        genericTest(minEx, false, false, true, true, true);
        maxIn = new DateIntervalConstraint(null, false, NOV4, true);
        genericTest(maxIn, true, true, true, true, false);
        maxEx = new DateIntervalConstraint(null, false, NOV4, false);
        genericTest(maxEx, true, true, true, false, false);
        any = new DateIntervalConstraint(null, true, null, true);
        genericTest(any, true, true, true, true, true);
        point = new DateIntervalConstraint(NOV2, true, NOV2, true);
        genericTest(point, false, true, false, false, false);
        failExPoint = new DateIntervalConstraint(NOV2, false, NOV2, false);
        genericTest(failExPoint, false, false, false, false, false);
        failInverted = new DateIntervalConstraint(NOV4, false, NOV2, false);
        genericTest(failInverted, false, false, false, false, false);
        failMax = new DateIntervalConstraint(NOV2, true, BAD, false);
        genericTest(failMax, false, true, true, true, true);
        failMin = new DateIntervalConstraint(BAD, false, NOV4, true);
        genericTest(failMin, true, true, true, true, false);
    }

    private void genericTest(DateIntervalConstraint constraint, boolean less, boolean lowerBound, boolean inner,
            boolean upperBound, boolean greater) {
        assertEquals(less, constraint.validate(NOV1));
        assertEquals(lowerBound, constraint.validate(NOV2));
        assertEquals(inner, constraint.validate(NOV3));
        assertEquals(upperBound, constraint.validate(NOV4));
        assertEquals(greater, constraint.validate(NOV5));
    }

    @Test
    public void testDateIntervalConstraintNullIsOk() {
        assertTrue(minInMaxEx.validate(null));
    }

    @Test
    public void testDateIntervalConstraintHandleStringYYYYMMDD() {
        assertTrue(minInMaxEx.validate(NOV2));
        assertFalse(minInMaxEx.validate("2014-11-01"));
    }

    @Test
    public void testDateIntervalConstraintHandleGregorianCalendar() {
        assertTrue(minInMaxEx.validate(new GregorianCalendar(2014, 10, 3)));
        assertFalse(minInMaxEx.validate(new GregorianCalendar(1984, 5, 15)));
    }

    @Test
    public void testDateIntervalConstraintHandleDate() throws ParseException {
        assertTrue(minInMaxEx.validate(new SimpleDateFormat("yyyy-MM-dd").parse(NOV3)));
        assertFalse(minInMaxEx.validate(new SimpleDateFormat("yyyy-MM-dd").parse(NOV1)));
    }

    @Test
    public void testDateIntervalConstraintHandleLongTimeMillis() throws ParseException {
        assertTrue(minInMaxEx.validate(new GregorianCalendar(2014, 10, 3).getTimeInMillis()));
        assertFalse(minInMaxEx.validate(new GregorianCalendar(1984, 5, 15).getTimeInMillis()));
    }

    @Test
    public void testDateIntervalConstraintDescription() throws ParseException {
        Constraint.Description description = minInMaxEx.getDescription();
        assertEquals("DateIntervalConstraint", description.getName());
        Map<String, Serializable> params = description.getParameters();
        assertEquals(4, params.size());
        assertTrue(params.containsKey("Minimum"));
        assertTrue(params.containsKey("MinimumInclusive"));
        assertTrue(params.containsKey("Maximum"));
        assertTrue(params.containsKey("MaximumInclusive"));
        assertEquals(new SimpleDateFormat("yyyy-MM-dd").parse(NOV2), params.get("Minimum"));
        assertTrue((Boolean) params.get("MinimumInclusive"));
        assertEquals(new SimpleDateFormat("yyyy-MM-dd").parse(NOV4), params.get("Maximum"));
        assertFalse((Boolean) params.get("MaximumInclusive"));
    }

    @Test
    public void testDateIntervalConstraintDescriptionOneBound() throws ParseException {
        Map<String, Serializable> params = minIn.getDescription().getParameters();
        assertTrue(params.containsKey("Minimum"));
        assertTrue(params.containsKey("MinimumInclusive"));
        assertEquals(2, params.size());
        assertEquals(new SimpleDateFormat("yyyy-MM-dd").parse(NOV2), params.get("Minimum"));
        assertTrue((Boolean) params.get("MinimumInclusive"));
    }

}
