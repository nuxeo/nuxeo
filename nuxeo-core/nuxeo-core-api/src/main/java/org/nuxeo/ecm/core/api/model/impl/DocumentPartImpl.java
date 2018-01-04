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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.model.impl;

import java.io.Serializable;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyVisitor;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DocumentPartImpl extends ComplexProperty implements DocumentPart {

    private static final long serialVersionUID = 1L;

    protected Schema schema;

    protected boolean clearComplexPropertyBeforeSet;

    public DocumentPartImpl(Schema schema) {
        super(null);
        this.schema = schema;
        // we pre-read this flag only once to avoid looking up and calling the SchemaManager many times
        clearComplexPropertyBeforeSet = Framework.getService(SchemaManager.class).getClearComplexPropertyBeforeSet();
    }

    @Override
    public void internalSetValue(Serializable value) throws PropertyException {
    }

    @Override
    public boolean isContainer() {
        return true;
    }

    @Override
    public Schema getSchema() {
        return schema;
    }

    @Override
    public String getName() {
        return schema.getName();
    }

    @Override
    public Schema getType() {
        return schema;
    }

    @Override
    public Field getField() {
        throw new UnsupportedOperationException("Document parts are not bound to schema fields");
    }

    @Override
    public Path collectPath(Path path) {
        return path;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public void accept(PropertyVisitor visitor, Object arg) throws PropertyException {
        visitChildren(visitor, arg);
    }

    @Override
    public Property createProperty(Property parent, Field field) {
        return createProperty(parent, field, 0);
    }

    @Override
    public Property createProperty(Property parent, Field field, int flags) {
        return PropertyFactory.createProperty(parent, field, flags);
    }

    @Override
    public boolean getClearComplexPropertyBeforeSet() {
        return clearComplexPropertyBeforeSet;
    }

    public boolean isSameAs(DocumentPart dp) {
        if (dp == null) {
            return false;
        }
        if (dp instanceof ComplexProperty) {
            return getNonPhantomChildren().equals(((ComplexProperty) dp).getNonPhantomChildren());
        }
        return false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + getName() + (isDirty() ? "*" : "") + ", " + children + ')';
    }

}
