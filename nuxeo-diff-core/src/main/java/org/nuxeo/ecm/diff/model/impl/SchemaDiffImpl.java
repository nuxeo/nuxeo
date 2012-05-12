/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ataillefer
 */
package org.nuxeo.ecm.diff.model.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.diff.model.PropertyDiff;
import org.nuxeo.ecm.diff.model.SchemaDiff;

/**
 * Implementation of SchemaDiff using a HashMap.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class SchemaDiffImpl implements SchemaDiff {

    private static final long serialVersionUID = -5117078340766697371L;

    /**
     * Map holding the schema diff.
     * <p>
     * Keys are field names. Values represent the difference between the left
     * doc and the right doc for the given field.
     */
    private Map<String, PropertyDiff> schemaDiff;

    /**
     * Instantiates a new schema diff impl.
     */
    public SchemaDiffImpl() {
        schemaDiff = new HashMap<String, PropertyDiff>();
    }

    public Map<String, PropertyDiff> getSchemaDiff() {
        return schemaDiff;
    }

    public int getFieldCount() {
        return schemaDiff.size();
    }

    public List<String> getFieldNames() {
        return new ArrayList<String>(schemaDiff.keySet());
    }

    public PropertyDiff getFieldDiff(String field) {
        return schemaDiff.get(field);
    }

    public PropertyDiff putFieldDiff(String field, PropertyDiff fieldDiff) {
        return schemaDiff.put(field, fieldDiff);
    }

}
