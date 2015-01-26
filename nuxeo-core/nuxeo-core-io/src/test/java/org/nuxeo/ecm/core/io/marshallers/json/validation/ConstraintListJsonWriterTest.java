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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.Test;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;
import org.nuxeo.ecm.core.schema.types.constraints.NotNullConstraint;
import org.nuxeo.ecm.core.schema.types.constraints.PatternConstraint;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@LocalDeploy("org.nuxeo.ecm.core.io:OSGI-INF/doc-type-contrib.xml")
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
