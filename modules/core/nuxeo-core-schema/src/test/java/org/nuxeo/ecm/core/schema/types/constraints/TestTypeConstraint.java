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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.core.schema.types.PrimitiveType;
import org.nuxeo.ecm.core.schema.types.TypeException;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;

public class TestTypeConstraint {

    private static final Number NUMBER = new Number() {
        private static final long serialVersionUID = 1L;

        @Override
        public long longValue() {
            return 12l;
        }

        @Override
        public int intValue() {
            return 12;
        }

        @Override
        public float floatValue() {
            return 12.7f;
        }

        @Override
        public double doubleValue() {
            return 12.7;
        }
    };

    private static final List<?> ANY_OBJECT = Collections.EMPTY_LIST;

    @Test
    public void testAnyTypeConstraintAcceptNull() {
        assertTrue(new TypeConstraint(new PrimitiveType("ImpossibleTypeConstraint") {
            private static final long serialVersionUID = 1L;

            @Override
            public Object convert(Object value) throws TypeException {
                return value;
            }

            @Override
            public boolean validate(Object object) {
                return false;
            }

            @Override
            public boolean support(Class<? extends Constraint> constraint) {
                return false;
            }
        }).validate(null));
    }

    @Test
    public void testBooleanAcceptPrimitiveBoolean() {
        assertTrue(new TypeConstraint(BooleanType.INSTANCE).validate(true));
    }

    @Test
    public void testBooleanAcceptBoolanObject() {
        assertTrue(new TypeConstraint(BooleanType.INSTANCE).validate(Boolean.FALSE));
    }

    @Test
    public void testBooleanDeniedBooleanAsString() {
        assertFalse(new TypeConstraint(BooleanType.INSTANCE).validate("false"));
    }

    @Test
    public void testBooleanDeniedStupidThings() {
        assertFalse(new TypeConstraint(BooleanType.INSTANCE).validate(ANY_OBJECT));
    }

    @Test
    public void testBinaryAcceptAnything() {
        assertTrue(new TypeConstraint(BinaryType.INSTANCE).validate(ANY_OBJECT));
    }

    @Test
    public void testStringAcceptAnything() {
        assertTrue(new TypeConstraint(StringType.INSTANCE).validate(ANY_OBJECT));
    }

    @Test
    public void testIntegerAcceptNumbers() {
        assertTrue(new TypeConstraint(IntegerType.INSTANCE).validate(NUMBER));
    }

    @Test
    public void testIntegerDeniedIntegerAsString() {
        assertFalse(new TypeConstraint(IntegerType.INSTANCE).validate("12"));
    }

    @Test
    public void testIntegerDeniedStupidThings() {
        assertFalse(new TypeConstraint(IntegerType.INSTANCE).validate(ANY_OBJECT));
    }

    @Test
    public void testLongAcceptNumbers() {
        assertTrue(new TypeConstraint(LongType.INSTANCE).validate(NUMBER));
    }

    @Test
    public void testLongDeniedIntegerAsString() {
        assertFalse(new TypeConstraint(LongType.INSTANCE).validate("12"));
    }

    @Test
    public void testLongDeniedStupidThings() {
        assertFalse(new TypeConstraint(LongType.INSTANCE).validate(ANY_OBJECT));
    }

    @Test
    public void testDoubleAcceptNumbers() {
        assertTrue(new TypeConstraint(DoubleType.INSTANCE).validate(NUMBER));
    }

    @Test
    public void testDoubleDeniedIntegerAsString() {
        assertFalse(new TypeConstraint(DoubleType.INSTANCE).validate("12"));
    }

    @Test
    public void testDoubleDeniedStupidThings() {
        assertFalse(new TypeConstraint(DoubleType.INSTANCE).validate(ANY_OBJECT));
    }

    @Test
    public void testDateAcceptDate() {
        assertTrue(new TypeConstraint(DateType.INSTANCE).validate(new Date()));
    }

    @Test
    public void testDateAcceptCalendar() {
        assertTrue(new TypeConstraint(DateType.INSTANCE).validate(new GregorianCalendar()));
    }

    @Test
    public void testDateDeniedDateAsString() {
        assertFalse(new TypeConstraint(DateType.INSTANCE).validate("2014-05-15"));
    }

    @Test
    public void testDateDeniedStupidThings() {
        assertFalse(new TypeConstraint(DateType.INSTANCE).validate(ANY_OBJECT));
    }

}
