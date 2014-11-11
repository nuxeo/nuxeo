/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.model.impl.osm;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.impl.ScalarProperty;
import org.nuxeo.ecm.core.schema.types.Field;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ScalarMemberProperty extends ScalarProperty {

    private static final long serialVersionUID = 1537310098432620929L;

    public ScalarMemberProperty(Property parent, Field field) {
        super(parent, field);
    }

    public ScalarMemberProperty(Property parent, Field field, int flags) {
        super(parent, field, flags);
    }

    @Override
    public void internalSetValue(Serializable value) throws PropertyException {
        ObjectAdapter adapter = ((Adaptable) parent).getAdapter();
        adapter.setValue(parent.getValue(), getName(), value);
    }

    @Override
    public Serializable internalGetValue() throws PropertyException {
        ObjectAdapter adapter = ((Adaptable) parent).getAdapter();
        Object value = adapter.getValue(parent.getValue(), getName());
        if (value != null && !(value instanceof Serializable)) {
            throw new PropertyException("Non serializable value: " + value);
        }
        return (Serializable) value;
    }

}
