/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.model.impl.osm;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;

import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.schema.types.Field;

/**
 * Phantom properties are not stored as children objects.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ObjectProperty extends ComplexMemberProperty {

    private static final long serialVersionUID = 5983583346105581835L;

    protected Serializable value;

    public ObjectProperty(ObjectAdapter adapter, Property parent, Field field) {
        super(adapter, parent, field);
    }

    public ObjectProperty(ObjectAdapter adapter, Property parent, Field field,
            int flags) {
        super(adapter, parent, field, flags);
    }

    @Override
    public Serializable internalGetValue() throws PropertyException {
        return value;
    }

    @Override
    public void internalSetValue(Serializable value) throws PropertyException {
        this.value = value;
    }

    @Override
    public final Object clone() throws CloneNotSupportedException {
        ObjectProperty clone = (ObjectProperty) super.clone();
        return clone;
    }

    private void readObject(ObjectInputStream in)
            throws ClassNotFoundException, IOException {
        // always perform the default de-serialization first
        in.defaultReadObject();
        children = new HashMap<String, Property>(); // initialize children
    }

}
