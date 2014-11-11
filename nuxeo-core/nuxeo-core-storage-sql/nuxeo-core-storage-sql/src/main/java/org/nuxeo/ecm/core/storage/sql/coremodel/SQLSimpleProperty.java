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

package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.SimpleProperty;

/**
 * A {@link SQLSimpleProperty} gives access to a wrapped SQL-level
 * {@link SimpleProperty}.
 *
 * @author Florent Guillaume
 */
public class SQLSimpleProperty extends SQLBaseProperty {

    private final SimpleProperty property;

    /**
     * Creates a {@link SQLSimpleProperty} to wrap a {@link SimpleProperty}.
     */
    public SQLSimpleProperty(SimpleProperty property, Type type,
            boolean readonly) {
        super(type, property.getName(), readonly);
        this.property = property;
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Property -----
     */

    @Override
    public String getName() {
        return property.getName();
    }

    @Override
    public Serializable getValue() throws DocumentException {
        try {
            return property.getValue();
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

    @Override
    public void setValue(Object value) throws DocumentException {
        if (!isSpecialSystemProperty(getName())) {
            checkWritable();
        }
        if (value != null && !(value instanceof Serializable)) {
            throw new DocumentException("Value is not Serializable: " + value);
        }
        try {
            property.setValue((Serializable) value);
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
    }

}
