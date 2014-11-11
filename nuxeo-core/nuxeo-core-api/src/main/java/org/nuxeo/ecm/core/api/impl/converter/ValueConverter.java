/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.impl.converter;

import java.util.Hashtable;
import java.util.Map;

import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.TypeException;
import org.nuxeo.runtime.api.Framework;

/**
 * Converts input values into values suitable to be set to a typed object.
 * <p>
 * Converters are associated with a type.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class ValueConverter {

    private static final SchemaManager typeManager = Framework.getLocalService(
            SchemaManager.class);

    private static final Map<String, ValueConverter> converters
            = new Hashtable<String, ValueConverter>();


    public abstract Object convert(Object value) throws TypeException;

    public static void addConvertor(String type, ValueConverter convertor) {
        converters.put(type, convertor);
    }

    public static void removeConvertor(String type) {
        converters.remove(type);
    }

    public static Object getValue(String type, Object value) throws TypeException {
        ValueConverter conv = converters.get(type);
        if (conv != null) {
            return conv.convert(value);
        } else {
            Type theType = typeManager.getType(type);
            if (theType == null) {
                throw new TypeException("No Such Type: " + type);
            }
            return theType.convert(value);
        }
    }

    static {
        converters.put(TypeConstants.CONTENT, BlobConverter.INSTANCE);
    }

}
