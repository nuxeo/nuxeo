/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.automation.core.doc.bonita;

import java.util.HashMap;

import org.nuxeo.ecm.automation.OperationDocumentation;

/**
 * Wrapper of a Nuxeo operation documentation for Bonita logic.
 *
 * @since 5.4.1
 */
public class BonitaOperationDocumentation {

    public static HashMap<String, String> TYPES_TO_BONITA_TYPES = new HashMap<String, String>();

    public static HashMap<String, String> TYPES_TO_BONITA_WIDGETS = new HashMap<String, String>();

    static {
        TYPES_TO_BONITA_TYPES.put("string", "string");
        TYPES_TO_BONITA_TYPES.put("boolean", "boolean");
        TYPES_TO_BONITA_TYPES.put("integer", "long");
        TYPES_TO_BONITA_TYPES.put("float", "long");
        TYPES_TO_BONITA_TYPES.put("date", "string"); // FIXME
        TYPES_TO_BONITA_TYPES.put("document", "string");
        TYPES_TO_BONITA_TYPES.put("documents", "string"); // TODO
        TYPES_TO_BONITA_TYPES.put("properties", "string"); // FIXME
        TYPES_TO_BONITA_TYPES.put("resource", "list"); // TODO

        TYPES_TO_BONITA_WIDGETS.put("string", "text");
        TYPES_TO_BONITA_WIDGETS.put("boolean", "checkbox");
        TYPES_TO_BONITA_WIDGETS.put("integer", "text");
        TYPES_TO_BONITA_WIDGETS.put("float", "text");
        TYPES_TO_BONITA_WIDGETS.put("date", "text");
        TYPES_TO_BONITA_WIDGETS.put("document", "text");
        TYPES_TO_BONITA_WIDGETS.put("documents", "text"); // TODO
        TYPES_TO_BONITA_WIDGETS.put("properties", "list");
        TYPES_TO_BONITA_WIDGETS.put("resource", "text"); // TODO
    }

    protected final OperationDocumentation operation;

    public BonitaOperationDocumentation(OperationDocumentation operation) {
        this.operation = operation;
    }

    public OperationDocumentation getOperation() {
        return operation;
    }

    public String getOperationInput() {
        // return only the first input for now
        return operation.getSignature()[0];
    }

    public String getOperationOutput() {
        // return only the first output for now
        return operation.getSignature()[1];
    }

    public String getConnectorId(String operationId) {
        return "Nuxeo" + operationId + "Connector";
    }

    public String getSetterName(String fieldName) {
        String res = "set" + fieldName.substring(0, 1).toUpperCase();
        if (fieldName.length() > 1) {
            res += fieldName.substring(1);
        }
        return res;
    }

    public String getGetterName(String fieldName) {
        String res = "get" + fieldName.substring(0, 1).toUpperCase();
        if (fieldName.length() > 1) {
            res += fieldName.substring(1);
        }
        return res;
    }

}
