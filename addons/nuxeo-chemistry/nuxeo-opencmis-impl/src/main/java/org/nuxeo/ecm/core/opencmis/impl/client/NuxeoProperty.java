/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.client;

import java.util.List;

import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.commons.api.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoPropertyData;

/**
 * Nuxeo Property, wrapping a NuxeoPropertyData.
 */
public class NuxeoProperty<T> implements Property<T> {

    private final NuxeoPropertyData<T> prop;

    public NuxeoProperty(NuxeoObject object, ObjectType type, String id) {
        prop = (NuxeoPropertyData<T>) object.data.getProperty(id);
    }

    public PropertyDefinition<T> getDefinition() {
        return prop.getPropertyDefinition();
    }

    public String getDisplayName() {
        return prop.getDisplayName();
    }

    public String getId() {
        return prop.getId();
    }

    public String getLocalName() {
        return prop.getLocalName();
    }

    public String getQueryName() {
        return prop.getQueryName();
    }

    public PropertyType getType() {
        return prop.getPropertyDefinition().getPropertyType();
    }

    public T getFirstValue() {
        return (T) prop.getFirstValue();
    }

    public String getValueAsString() {
        return String.valueOf(getFirstValue());
    }

    public List<T> getValues() {
        return (List<T>) prop.getValues();
    }

    public boolean isMultiValued() {
        return prop.getPropertyDefinition().getCardinality() == Cardinality.MULTI;
    }

    public List<Object> getExtensions() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void setExtensions(List<Object> extensions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

}
