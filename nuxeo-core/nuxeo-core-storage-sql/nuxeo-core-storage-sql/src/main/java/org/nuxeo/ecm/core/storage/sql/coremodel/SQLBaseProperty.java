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

import java.util.Collection;
import java.util.Iterator;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.schema.types.Type;

/**
 * @author Florent Guillaume
 */
public abstract class SQLBaseProperty implements Property {

    protected final Type type;

    /**
     * The toplevel document containing this property (or null for documents
     * themselves -- use {@link #getDocument} to access).
     */
    protected final SQLDocument doc;

    protected final String name;

    public SQLBaseProperty(Type type, String name, SQLDocument doc) {
        this.type = type;
        this.doc = doc;
        this.name = name;
    }

    /**
     * Gets the toplevel document containing this property.
     *
     * @since 5.9.2
     */
    public SQLDocument getDocument() {
        return doc;
    }

    public SQLSession getSession() {
        return (SQLSession) getDocument().getSession();
    }

    public void checkWritable() throws DocumentException {
        getDocument().checkWritable(name);
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Property -----
     */

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public boolean isNull() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNull() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPropertySet(String name) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property getProperty(String name) throws DocumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Property> getProperties() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Property> getPropertyIterator() throws DocumentException {
        throw new UnsupportedOperationException();
    }

}
