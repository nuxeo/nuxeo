/*
 * Copyright 2009-2010 Nuxeo SA <http://nuxeo.com>
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
package org.nuxeo.ecm.core.opencmis.impl.server;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.apache.chemistry.opencmis.commons.api.PropertyData;
import org.apache.chemistry.opencmis.commons.api.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Base abstract class for a live property of an object.
 *
 * @see NuxeoPropertyData
 */
public abstract class NuxeoPropertyDataBase<T> implements PropertyData<T> {

    protected final PropertyDefinition<T> propertyDefinition;

    protected final DocumentModel doc;

    public NuxeoPropertyDataBase(PropertyDefinition<T> propertyDefinition,
            DocumentModel doc) {
        this.propertyDefinition = propertyDefinition;
        this.doc = doc;
    }

    public PropertyDefinition<T> getPropertyDefinition() {
        return propertyDefinition;
    }

    public String getId() {
        return propertyDefinition.getId();
    }

    public String getLocalName() {
        return propertyDefinition.getLocalName();
    }

    public String getDisplayName() {
        return propertyDefinition.getDisplayName();
    }

    public String getQueryName() {
        return propertyDefinition.getQueryName();
    }

    public List<T> getValues() {
        return Collections.singletonList(getFirstValue());
    }

    public void setValue(Object value) {
        Serializable old = null;
        if (value == null && old == null) {
            return;
        }
        if (value != null && value.equals(old)) {
            return;
        }
        throw new CmisConstraintException("Read-only property: "
                + propertyDefinition.getId());
    }

    public List<Object> getExtensions() {
        return null;
    }

    public void setExtensions(List<Object> extensions) {
        throw new UnsupportedOperationException();
    }

}
