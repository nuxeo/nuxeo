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

package org.nuxeo.ecm.core.api.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.impl.AbstractProperty;
import org.nuxeo.ecm.core.api.model.impl.DocumentPartImpl;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.runtime.api.Framework;

/**
 * Data model implementation.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DataModelImpl implements DataModel {

    private static final long serialVersionUID = 1L;

    private final DocumentPart dp;

    /**
     * Builds an empty data model.
     *
     * @param schema a schema name.
     */
    public DataModelImpl(String schema) {
        this(schema, new HashMap<String, Object>());
    }

    /**
     * Builds a data model using the given data.
     *
     * @param schema a schema name.
     * @param data the data (map String&gt;Object) to put in the DataModel.
     */
    public DataModelImpl(String schemaName, Map<String, Object> data) {
        assert data != null;
        SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
        Schema schema = schemaManager.getSchema(schemaName);
        dp = new DocumentPartImpl(schema);
        if (!data.isEmpty()) {
            try {
                dp.init((Serializable) data);
            } catch (PropertyException e) {
                throw new ClientRuntimeException(e);
            }
        }
    }

    public DataModelImpl(DocumentPart part) {
        assert part != null;
        dp = part;
    }

    /**
     * Gets the underlying document part.
     */
    public DocumentPart getDocumentPart() {
        return dp;
    }

    @Override
    public String getSchema() {
        return dp.getSchema().getName();
    }

    @Override
    public Object getData(String key) throws PropertyException {
        return dp.getValue(key);
    }

    @Override
    public void setData(String key, Object value) throws PropertyException {
        dp.setValue(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMap() throws PropertyException {
        return (Map<String, Object>) dp.getValue();
    }

    @Override
    public void setMap(Map<String, Object> data) throws PropertyException {
        dp.setValue(data);
    }

    @Override
    public boolean isDirty() {
        return dp.isDirty();
    }

    @Override
    public boolean isDirty(String name) throws PropertyNotFoundException {
        return dp.get(name).isDirty();
    }

    @Override
    public Collection<String> getDirtyFields() {
        Collection<String> dirtyFields = new ArrayList<String>();
        for (Property prop : dp.getChildren()) {
            if (prop.isDirty()) {
                dirtyFields.add(prop.getName());
            }
        }
        return dirtyFields;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + getSchema()
                + (dp.isDirty() ? "*" : "") + ')';
    }

    @Override
    public void setDirty(String name) throws PropertyNotFoundException {
        ((AbstractProperty) dp.get(name)).setIsModified();
    }

    @Override
    public Object getValue(String path) throws PropertyException {
        return dp.getValue(path);
    }

    @Override
    public Object setValue(String path, Object value) throws PropertyException {
        Property prop = dp.resolvePath(path);
        Object oldValue = prop.getValue();
        prop.setValue(value);
        return oldValue;
    }

}
