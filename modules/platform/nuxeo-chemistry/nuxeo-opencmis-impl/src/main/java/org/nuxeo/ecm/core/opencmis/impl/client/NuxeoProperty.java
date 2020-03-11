/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.client;

import java.util.Iterator;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoPropertyData;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoPropertyDataBase;

/**
 * Live Nuxeo document property, wrapping a {@link NuxeoPropertyData}.
 */
public class NuxeoProperty<T> implements Property<T> {

    private final NuxeoPropertyDataBase<T> prop;

    @SuppressWarnings("unchecked")
    public NuxeoProperty(NuxeoObject object, String id) {
        prop = (NuxeoPropertyDataBase<T>) object.data.getProperty(id);
    }

    @Override
    public PropertyDefinition<T> getDefinition() {
        return prop.getPropertyDefinition();
    }

    @Override
    public String getDisplayName() {
        return prop.getDisplayName();
    }

    @Override
    public String getId() {
        return prop.getId();
    }

    @Override
    public String getLocalName() {
        return prop.getLocalName();
    }

    @Override
    public String getQueryName() {
        return prop.getQueryName();
    }

    @Override
    public PropertyType getType() {
        return prop.getPropertyDefinition().getPropertyType();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U> U getValue() {
        // cast needed by Sun compiler
        return (U) prop.getValue();
    }

    @Override
    public T getFirstValue() {
        return prop.getFirstValue();
    }

    @Override
    public String getValueAsString() {
        return String.valueOf(getFirstValue());
    }

    @Override
    public String getValuesAsString() {
        StringBuilder buf = new StringBuilder();
        buf.append('[');
        for (Iterator<T> it = getValues().iterator(); it.hasNext();) {
            buf.append(String.valueOf(it.next()));
            if (it.hasNext()) {
                buf.append(", ");
            }
        }
        buf.append(']');
        return buf.toString();
    }

    @Override
    public List<T> getValues() {
        return prop.getValues();
    }

    @Override
    public boolean isMultiValued() {
        return prop.getPropertyDefinition().getCardinality() == Cardinality.MULTI;
    }

    @Override
    public List<CmisExtensionElement> getExtensions() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void setExtensions(List<CmisExtensionElement> extensions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

}
