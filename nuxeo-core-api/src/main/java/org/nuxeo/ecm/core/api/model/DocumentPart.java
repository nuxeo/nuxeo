/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

    /**
     * Exports as a map the document values. Only the non phantom properties are exported.
     *
     * @return
     */
    Map<String, Serializable> exportValues() throws PropertyException;

    void importValues(Map<String, Serializable> values)  throws PropertyException;

    PropertyDiff exportDiff() throws PropertyException;

    void importDiff(PropertyDiff diff) throws PropertyException;

//
//    public void setContextData(String key, Object value);
//
//    public Object getContextData(String key);

}
