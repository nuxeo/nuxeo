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

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.validation.ConstraintViolation;
import org.nuxeo.ecm.core.api.validation.ConstraintViolation.PathNode;
import org.nuxeo.ecm.core.api.validation.DocumentValidationReport;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;

import com.thoughtworks.xstream.io.json.JsonWriter;

/**
 * Convert {@link DocumentValidationReport} to Json.
 * <p>
 * This marshaller is enrichable: register class implementing {@link AbstractJsonEnricher} and managing
 * {@link DocumentValidationReport}.
 * </p>
 * <p>
 * This marshaller is also extensible: extend it and simply override
 * {@link ExtensibleEntityJsonWriter#extend(DocumentValidationReport, JsonWriter)}.
 * </p>
 * <p>
 * Format is:
 *
 * <pre>
 * {@code
 * {
 *   "entity-type":"validation_report",
 *   "has_error": true|false,
 *   "number": 123, <- number of errors present in this report
 *   "violations": [
 *     {
 *       "message": "TheErrorMessage",
 *       "invalid_value": null|"THE_INVALID_VALUE_AS_STRING",
 *       "constraint": {
 *         see {@link ConstraintJsonWriter} format
 *       }
 *     },
 *     ...
 *   ]
 *             <-- contextParameters if there are enrichers activated
 *             <-- additional property provided by extend() method
 * }
 * </pre>
 *
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DocumentValidationReportJsonWriter extends ExtensibleEntityJsonWriter<DocumentValidationReport> {

    public static final String ENTITY_TYPE = "validation_report";

    public DocumentValidationReportJsonWriter() {
        super(ENTITY_TYPE, DocumentValidationReport.class);
    }

    @Override
    protected void writeEntityBody(DocumentValidationReport report, JsonGenerator jg) throws IOException {
        jg.writeBooleanField("has_error", report.hasError());
        jg.writeNumberField("number", report.numberOfErrors());
        // constraint violations
        jg.writeArrayFieldStart("violations");
        for (ConstraintViolation violation : report.asList()) {
            jg.writeStartObject();
            // violation message
            String message = violation.getMessage(ctx.getLocale());
            jg.writeStringField("message", message);
            // invalid value
            Object invalidValue = violation.getInvalidValue();
            if (invalidValue == null) {
                jg.writeNullField("invalid_value");
            } else {
                jg.writeStringField("invalid_value", invalidValue.toString());
            }
            // violated constraint
            Constraint constraint = violation.getConstraint();
            writeEntityField("constraint", constraint, jg);
            // violation place
            jg.writeArrayFieldStart("path");
            for (PathNode node : violation.getPath()) {
                jg.writeStartObject();
                jg.writeStringField("field_name", node.getField().getName().getPrefixedName());
                jg.writeBooleanField("is_list_item", node.isListItem());
                if (node.isListItem()) {
                    jg.writeNumberField("index", node.getIndex());
                }
                jg.writeEndObject();
            }
            jg.writeEndArray();
            jg.writeEndObject();
        }
        jg.writeEndArray();
    }

}