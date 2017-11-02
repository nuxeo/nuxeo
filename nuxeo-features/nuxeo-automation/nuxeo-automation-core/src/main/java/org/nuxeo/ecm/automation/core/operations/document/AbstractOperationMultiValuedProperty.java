/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benjamin JALON <bjalon@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.document;

import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.TypeException;

/**
 * Abstract Class that exposes some useful method to manage list of values
 *
 * @author <a href="mailto:bjalon@nuxeo.com">Benjamin JALON</a>
 * @since 5.7
 */
public class AbstractOperationMultiValuedProperty {

    /**
     * Check if the given field type store a list of values and if the given value is compatible with the given type. We
     * assume the Type store a list of scalar values, not complex types.
     */
    protected void checkFieldType(Type type, Object value) throws OperationException {
        if (!type.isListType()) {
            throw new OperationException("Only multivalued types can be set using this operation");
        }

        ListType listType = (ListType) type;
        Type itemType = listType.getFieldType();
        if (itemType.isComplexType()) {
            throw new UnsupportedOperationException("Manage only lists of scalar items");
        }

        try {
            if (itemType.convert(value) == null) {
                String exceptionReason = String.format("Given type \"%s\" value is not a %s type", value,
                        itemType.getName());
                throw new UnsupportedOperationException(exceptionReason);
            }
        } catch (TypeException | ClassCastException e) {
            String exceptionReason = String.format("Given type \"%s\" value is not a %s type", value,
                    itemType.getName());
            throw new OperationException(exceptionReason, e);
        }
    }
}
