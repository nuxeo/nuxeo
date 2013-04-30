/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Benjamin JALON <bjalon@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.document;

import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Type;

/**
 * Abstract Class that exposes some useful method to manage list of values
 *
 * @author <a href="mailto:bjalon@nuxeo.com">Benjamin JALON</a>
 * @since 5.7
 */
public class AbstractOperationMultiValuedProperty {

    /**
     * Check if the given field type store a list of values and if the given
     * value is compatible with the given type.
     * We assume the Type store a list of scalar values, not complex types.
     */
    protected void checkFieldType(Type type, Object value)
            throws OperationException {
        if (!type.isListType()) {
            throw new OperationException(
                    "Only multivalued String Types can be set using this operation");
        }

        ListType listType = (ListType) type;
        Type itemType = listType.getFieldType();
        if (itemType.isComplexType()) {
            throw new UnsupportedOperationException(
                    "Manage only lists of scalar items");
        }

        if (!itemType.newInstance().getClass().equals(value.getClass())) {
            throw new UnsupportedOperationException(String.format(
                    "Given type \"%s\" value is not a %s type", value,
                    itemType.getName()));
        }
    }
}
