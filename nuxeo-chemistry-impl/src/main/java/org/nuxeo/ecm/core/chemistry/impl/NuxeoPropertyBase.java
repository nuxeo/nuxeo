/*
 * Copyright 2009 Nuxeo SA <http://nuxeo.com>
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
 * Authors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.chemistry.impl;

import java.io.Serializable;

import org.apache.chemistry.ConstraintViolationException;
import org.apache.chemistry.Property;
import org.apache.chemistry.PropertyDefinition;

public abstract class NuxeoPropertyBase implements Property {

    protected final PropertyDefinition propertyDefinition;

    protected final DocumentModelHolder docHolder;

    public NuxeoPropertyBase(PropertyDefinition propertyDefinition,
            DocumentModelHolder docHolder) {
        this.docHolder = docHolder;
        this.propertyDefinition = propertyDefinition;
    }

    public PropertyDefinition getDefinition() {
        return propertyDefinition;
    }

    public void setValue(Serializable value) {
        Serializable old = getValue();
        if (value == null && old == null) {
            return;
        }
        if (value != null && value.equals(old)) {
            return;
        }
        throw new ConstraintViolationException("Read-only property: "
                + propertyDefinition.getId());
    }

}
