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
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.core.api.validation.ConstraintViolation;
import org.nuxeo.ecm.core.api.validation.ConstraintViolation.PathNode;
import org.nuxeo.ecm.core.api.validation.DocumentValidationReport;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.constraints.NotNullConstraint;
import org.nuxeo.ecm.core.schema.types.constraints.PatternConstraint;

import javax.inject.Inject;

public class DocumentValidationReportJsonWriterTest extends
        AbstractJsonWriterTest.Local<DocumentValidationReportJsonWriter, DocumentValidationReport> {

    public DocumentValidationReportJsonWriterTest() {
        super(DocumentValidationReportJsonWriter.class, DocumentValidationReport.class);
    }

    @Inject
    private SchemaManager schemaManager;

    @Test
    public void test() throws Exception {
        Schema schema = schemaManager.getSchema("dublincore");
        Field title = schema.getField("title");
        List<PathNode> titleNode = Arrays.asList(new PathNode(title));
        ConstraintViolation violation1 = new ConstraintViolation(schema, titleNode, NotNullConstraint.get(), null);
        Field description = schema.getField("description");
        List<PathNode> descNode = Arrays.asList(new PathNode(description));
        ConstraintViolation violation2 = new ConstraintViolation(schema, descNode, new PatternConstraint(".*"), null);
        DocumentValidationReport report = new DocumentValidationReport(Arrays.asList(violation1, violation2));
        JsonAssert json = jsonAssert(report);
        json.properties(4);
        json.has("entity-type").isEquals("validation_report");
        json.has("has_error").isTrue();
        json.has("number").isEquals(2);
        json = json.has("violations").length(2);
        json.has(0).properties(4).has("message").isText();
        json.has(1).properties(4).has("message").isText();
        json.childrenContains("invalid_value", null, null);
        json.childrenContains("constraint.entity-type", "validation_constraint", "validation_constraint");
        json.childrenContains("constraint.name", "NotNullConstraint", "PatternConstraint");
        json.childrenContains("path.field_name", "dc:title", "dc:description");
        json.childrenContains("path.is_list_item", "false", "false");
    }

    @Test
    public void testNoErrors() throws Exception {
        DocumentValidationReport report = new DocumentValidationReport(new ArrayList<ConstraintViolation>());
        JsonAssert json = jsonAssert(report);
        json.properties(4);
        json.has("entity-type").isEquals("validation_report");
        json.has("has_error").isFalse();
        json.has("number").isEquals(0);
        json = json.has("violations").length(0);
    }

}
