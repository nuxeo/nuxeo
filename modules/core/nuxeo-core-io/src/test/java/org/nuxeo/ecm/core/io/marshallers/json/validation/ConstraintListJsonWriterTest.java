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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.Test;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;
import org.nuxeo.ecm.core.schema.types.constraints.NotNullConstraint;
import org.nuxeo.ecm.core.schema.types.constraints.PatternConstraint;

public class ConstraintListJsonWriterTest extends
        AbstractJsonWriterTest.Local<ConstraintListJsonWriter, List<Constraint>> {

    public ConstraintListJsonWriterTest() {
        super(ConstraintListJsonWriter.class, List.class, TypeUtils.parameterize(List.class, Constraint.class));
    }

    public List<Constraint> getElements() {
        List<Constraint> constraints = new ArrayList<>();
        constraints.add(NotNullConstraint.get());
        constraints.add(new PatternConstraint(".*"));
        return constraints;
    }

    @Test
    public void test() throws Exception {
        List<Constraint> elements = getElements();
        JsonAssert json = jsonAssert(elements);
        json.isObject();
        json.properties(2);
        json.has("entity-type").isEquals("validation_constraints");
        json = json.has("entries").length(elements.size());
        json.childrenContains("entity-type", "validation_constraint", "validation_constraint");
        json.childrenContains("name", "NotNullConstraint", "PatternConstraint");
    }

}
