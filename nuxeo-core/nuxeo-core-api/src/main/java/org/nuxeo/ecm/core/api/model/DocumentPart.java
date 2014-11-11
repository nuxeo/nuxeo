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

package org.nuxeo.ecm.core.api.model;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;

/**
 * A document part is the root of a property tree which is specified by a schema
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface DocumentPart extends Property {

    /**
     * The document schema.
     *
     * @return the schema
     */
    @Override
    Schema getSchema();

    Property createProperty(Property parent, Field field);

    Property createProperty(Property parent, Field field, int flags);

    PropertyDiff exportDiff() throws PropertyException;

    void importDiff(PropertyDiff diff) throws PropertyException;

//
//    public void setContextData(String key, Object value);
//
//    public Object getContextData(String key);

}
