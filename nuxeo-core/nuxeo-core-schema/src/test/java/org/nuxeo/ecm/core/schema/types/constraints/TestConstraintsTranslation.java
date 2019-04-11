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
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

import org.junit.Test;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.schema.types.resolver.TestingColorResolver;

public class TestConstraintsTranslation {

    @Test
    public void testGenericMessage() {
        AbstractConstraint constraint = new AbstractConstraint() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean validate(Object object) {
                return true;
            }

            @Override
            public Description getDescription() {
                HashMap<String, Serializable> parameters = new HashMap<>();
                parameters.put("param1", "value1");
                parameters.put("param2", "value2");
                return new Description("AlwaysWrongConstraint", parameters);
            }
        };
        checkConstraintMessage(constraint);
    }

    @Test
    public void testNotNullContraintMessage() {
        checkConstraintMessage(NotNullConstraint.get());
    }

    @Test
    public void testPatternContraintMessage() {
        checkConstraintMessage(new PatternConstraint(".*\\S.*"));
    }

    @Test
    public void testEnumContraintMessage() {
        checkConstraintMessage(new EnumConstraint("tic", "tac"));
    }

    @Test
    public void testLengthContraintMinMaxMessage() {
        checkConstraintMessage(new LengthConstraint(2, 10));
    }

    @Test
    public void testLengthContraintMinMessage() {
        checkConstraintMessage(new LengthConstraint(2, null));
    }

    @Test
    public void testLengthContraintMaxMessage() {
        checkConstraintMessage(new LengthConstraint(null, 10));
    }

    @Test
    public void testNumericIntervalContraintMinInMaxInMessage() {
        checkConstraintMessage(new NumericIntervalConstraint(2, true, 10, true));
    }

    @Test
    public void testNumericIntervalContraintMinInMaxExMessage() {
        checkConstraintMessage(new NumericIntervalConstraint(2, true, 10, false));
    }

    @Test
    public void testNumericIntervalContraintMinExMaxInMessage() {
        checkConstraintMessage(new NumericIntervalConstraint(2, false, 10, true));
    }

    @Test
    public void testNumericIntervalContraintMinExMaxExMessage() {
        checkConstraintMessage(new NumericIntervalConstraint(2, false, 10, false));
    }

    @Test
    public void testNumericIntervalContraintMinInMessage() {
        checkConstraintMessage(new NumericIntervalConstraint(2, true, null, true));
    }

    @Test
    public void testNumericIntervalContraintMinExMessage() {
        checkConstraintMessage(new NumericIntervalConstraint(2, false, null, true));
    }

    @Test
    public void testNumericIntervalContraintMaxInMessage() {
        checkConstraintMessage(new NumericIntervalConstraint(null, true, 10, true));
    }

    @Test
    public void testNumericIntervalContraintMaxExMessage() {
        checkConstraintMessage(new NumericIntervalConstraint(null, true, 10, false));
    }

    @Test
    public void testDateIntervalContraintMinInMaxInMessage() {
        checkConstraintMessage(new DateIntervalConstraint("2014-11-02", true, "2014-11-10", true));
    }

    @Test
    public void testDateIntervalContraintMinInMaxExMessage() {
        checkConstraintMessage(new DateIntervalConstraint("2014-11-02", true, "2014-11-10", false));
    }

    @Test
    public void testDateIntervalContraintMinExMaxInMessage() {
        checkConstraintMessage(new DateIntervalConstraint("2014-11-02", false, "2014-11-10", true));
    }

    @Test
    public void testDateIntervalContraintMinExMaxExMessage() {
        checkConstraintMessage(new DateIntervalConstraint("2014-11-02", false, "2014-11-10", false));
    }

    @Test
    public void testDateIntervalContraintMinInMessage() {
        checkConstraintMessage(new DateIntervalConstraint("2014-11-02", true, null, true));
    }

    @Test
    public void testDateIntervalContraintMinExMessage() {
        checkConstraintMessage(new DateIntervalConstraint("2014-11-02", false, null, true));
    }

    @Test
    public void testDateIntervalContraintMaxInMessage() {
        checkConstraintMessage(new DateIntervalConstraint(null, true, "2014-11-10", true));
    }

    @Test
    public void testDateIntervalContraintMaxExMessage() {
        checkConstraintMessage(new DateIntervalConstraint(null, true, "2014-11-10", false));
    }

    @Test
    public void testTypeConstraintBinaryMessage() {
        checkConstraintMessage(new TypeConstraint(BinaryType.INSTANCE));
    }

    @Test
    public void testTypeConstraintBooleanMessage() {
        checkConstraintMessage(new TypeConstraint(BooleanType.INSTANCE));
    }

    @Test
    public void testTypeConstraintIntegerMessage() {
        checkConstraintMessage(new TypeConstraint(IntegerType.INSTANCE));
    }

    @Test
    public void testTypeConstraintLongMessage() {
        checkConstraintMessage(new TypeConstraint(LongType.INSTANCE));
    }

    @Test
    public void testTypeConstraintDateMessage() {
        checkConstraintMessage(new TypeConstraint(DateType.INSTANCE));
    }

    @Test
    public void testTypeConstraintStringMessage() {
        checkConstraintMessage(new TypeConstraint(StringType.INSTANCE));
    }

    @Test
    public void testTypeConstraintDoubleMessage() {
        checkConstraintMessage(new TypeConstraint(DoubleType.INSTANCE));
    }

    enum Color {
        RED, GREEN, BLUE;
    }

    @Test
    public void testExternalReferenceConstraintMessage() {
        TestingColorResolver resolver = new TestingColorResolver();
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(TestingColorResolver.COLOR_MODE,
                TestingColorResolver.MODE.PRIMARY.name());
        resolver.configure(parameters);
        checkConstraintMessage(new ObjectResolverConstraint(resolver));
    }

    private void checkConstraintMessage(Constraint constraint) {
        for (Locale locale : Arrays.asList(Locale.FRENCH, Locale.ENGLISH)) {
            String message = constraint.getErrorMessage("  ", locale);
            assertNotNull(message);
            assertFalse(message.isEmpty());
            assertFalse(message.contains(Constraint.MESSAGES_KEY));
            System.out.println(String.format("[%s] %s : %s", locale.toString(), constraint.getDescription().getName(),
                    message));
        }
    }

}
