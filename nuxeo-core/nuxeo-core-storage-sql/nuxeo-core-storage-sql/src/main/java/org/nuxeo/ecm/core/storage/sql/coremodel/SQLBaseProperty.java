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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.storage.sql.Model;

/**
 * @author Florent Guillaume
 */
public abstract class SQLBaseProperty implements Property {

    protected final Type type;

    protected final boolean readonly;

    public static final String DC_ISSUED = "dc:issued";

    // authorize update of aggregated text from related resources (comments,
    // annotations, tags, ...)
    public static final String RELATED_TEXT_RESOURCES = "relatedtextresources";

    protected static final Set<String> VERSION_WRITABLE_PROPS = new HashSet<String>(
            Arrays.asList( //
                    Model.FULLTEXT_JOBID_PROP, //
                    Model.FULLTEXT_BINARYTEXT_PROP, //
                    Model.MISC_LIFECYCLE_STATE_PROP, //
                    Model.LOCK_OWNER_PROP, //
                    Model.LOCK_CREATED_PROP, //
                    DC_ISSUED, //
                    RELATED_TEXT_RESOURCES //
            ));

    public static boolean isSpecialSystemProperty(String name) {
        return name != null && ( VERSION_WRITABLE_PROPS.contains(name)
                || name.startsWith(Model.FULLTEXT_BINARYTEXT_PROP));
    }

    public SQLBaseProperty(Type type, String name, boolean readonly) {
        this.type = type;
        if (isSpecialSystemProperty(name)) {
            // special handling of system properties
            this.readonly = false;
        } else {
            this.readonly = readonly;
        }
    }

    // for SQLDocument
    public void checkWritable() throws DocumentException {
        if (readonly) {
            throw new DocumentException("Cannot write property: " + getName());
        }
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
