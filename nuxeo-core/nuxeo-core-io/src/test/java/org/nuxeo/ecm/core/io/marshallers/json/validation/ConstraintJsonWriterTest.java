/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.io.marshallers.json.validation;

import org.junit.Test;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;
import org.nuxeo.ecm.core.schema.types.constraints.NotNullConstraint;
import org.nuxeo.ecm.core.schema.types.constraints.NumericIntervalConstraint;

public class ConstraintJsonWriterTest extends AbstractJsonWriterTest.Local<ConstraintJsonWriter, Constraint> {

    public ConstraintJsonWriterTest() {
        super(ConstraintJsonWriter.class, Constraint.class);
    }

    @Test
    public void testWithParameters() throws Exception {
        Constraint constraint = new NumericIntervalConstraint(10, true, 20, false);
        JsonAssert json = jsonAssert(constraint);
        json.properties(3);
        json.has("entity-type").isEquals("validation_constraint");
        json.has("name").isEquals("NumericIntervalConstraint");
        json = json.has("parameters").properties(4);
        json.has("Minimum").isEquals("10");
        json.has("MinimumInclusive").isEquals("true");
        json.has("Maximum").isEquals("20");
        json.has("MaximumInclusive").isEquals("false");
    }

    @Test
    public void testWithoutParameter() throws Exception {
        Constraint constraint = NotNullConstraint.get();
        JsonAssert json = jsonAssert(constraint);
        json = json.has("parameters").properties(0);
    }

}
