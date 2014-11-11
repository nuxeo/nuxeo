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

package org.nuxeo.ecm.core.api.model.impl;

import java.io.Serializable;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyDiff;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyVisitor;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;

/**
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentPartImpl extends ComplexProperty implements DocumentPart {

    private static final long serialVersionUID = 1L;

    protected Schema schema;

    public DocumentPartImpl(Schema schema) {
        super(null);
        this.schema = schema;
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
        throw new UnsupportedOperationException(
                "Document parts are not bound to schema fields");
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
    public PropertyDiff exportDiff() {
        return null;
    }

    @Override
    public void importDiff(PropertyDiff diff) {
    }

    public boolean isSameAs(DocumentPart dp) {
        if (dp == null) {
            return false;
        }
        if (dp instanceof ComplexProperty) {
            return getNonPhantomChildren().equals(
                    ((ComplexProperty) dp).getNonPhantomChildren());
        }
        return false;
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + getName()
                + (isDirty() ? "*" : "") + ", " + children + ')';
    }

}
