/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.client;

import java.util.Iterator;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.ObjectType;
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
    public NuxeoProperty(NuxeoObject object, ObjectType type, String id) {
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
        return (T) prop.getFirstValue();
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
        return (List<T>) prop.getValues();
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
