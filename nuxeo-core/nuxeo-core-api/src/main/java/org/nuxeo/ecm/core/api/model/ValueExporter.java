/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.model.impl.MapProperty;
import org.nuxeo.ecm.core.api.model.impl.ScalarProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.schema.types.QName;

/**
 * Exporter for a document's values into a map.
 * <p>
 * The values of the first-level keys of the map may be prefixed (standard
 * prefix:name naming) or not.
 */
@SuppressWarnings("unchecked")
public class ValueExporter implements PropertyVisitor {

    private final Map<String, Serializable> result = new HashMap<String, Serializable>();

    private final boolean prefixed;

    /**
     * Constructs an exporter.
     *
     * @param prefixed whether first-level keys of the map are prefixed
     */
    public ValueExporter(boolean prefixed) {
        this.prefixed = prefixed;
    }

    public Map<String, Serializable> getResult() {
        return result;
    }

    public Map<String, Serializable> run(DocumentPart dp)
            throws PropertyException {
        dp.accept(this, result);
        return result;
    }

    protected String getName(Property property) {
        QName name = property.getField().getName();
        return prefixed ? name.getPrefixedName() : name.getLocalName();
    }

    @Override
    public boolean acceptPhantoms() {
        return false;
    }

    @Override
    public Object visit(MapProperty property, Object arg)
            throws PropertyException {

        Serializable value;
        if (property.isContainer()) {
            value = new HashMap<String, Serializable>();
        } else {
            value = property.getValue();
        }

        if (BlobProperty.class.isAssignableFrom(property.getClass())) {
            value = property.getValue();
            if (property.getParent().isList()) {
                ((Collection<Serializable>) arg).add(value);
            } else {
                ((Map<String, Serializable>) arg).put(getName(property), value);
            }
            return null;
        } else if (property.getParent().isList()) {
            ((Collection<Serializable>) arg).add(value);
        } else {
            ((Map<String, Serializable>) arg).put(getName(property), value);
        }
        return value;
    }

    @Override
    public Object visit(ListProperty property, Object arg)
            throws PropertyException {
        Serializable value;
        if (property.isContainer()) {
            value = new ArrayList<Serializable>();
        } else {
            value = property.getValue();
        }
        if (property.getParent().isList()) {
            ((Collection<Serializable>) arg).add(value);
        } else {
            ((Map<String, Serializable>) arg).put(getName(property), value);
        }
        return value;
    }

    @Override
    public Object visit(ScalarProperty property, Object arg)
            throws PropertyException {
        Serializable value = property.getValue();
        if (property.getParent().isList()) {
            ((Collection<Serializable>) arg).add(value);
        } else {
            ((Map<String, Serializable>) arg).put(getName(property), value);
        }
        return null;
    }

}
