/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
